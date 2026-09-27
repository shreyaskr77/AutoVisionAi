package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.data.repository.AnalysisProgress
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VehicleRepository
import com.example.domain.model.AnalysisResult
import com.example.domain.model.AnalysisStage
import com.example.domain.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Loading(val stage: AnalysisStage) : AnalysisUiState()
    data class Success(val result: AnalysisResult) : AnalysisUiState()
    data class Error(val message: String, val canFallback: Boolean = false) : AnalysisUiState()
}

class AutoVisionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val settingsRepository = SettingsRepository(application)
    private val repository = VehicleRepository(application, database.scanDao(), settingsRepository)

    val serverUrl: StateFlow<String> = settingsRepository.serverUrl

    val allScans: StateFlow<List<ScanEntity>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScans: StateFlow<List<ScanEntity>> = repository.recentScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedImageFile = MutableStateFlow<File?>(null)
    val selectedImageFile: StateFlow<File?> = _selectedImageFile.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _selectedVehicleId = MutableStateFlow(1)
    val selectedVehicleId: StateFlow<Int> = _selectedVehicleId.asStateFlow()

    private val _isBackendOnline = MutableStateFlow<Boolean?>(null)
    val isBackendOnline: StateFlow<Boolean?> = _isBackendOnline.asStateFlow()

    private val _isSavedCurrentScan = MutableStateFlow(false)
    val isSavedCurrentScan: StateFlow<Boolean> = _isSavedCurrentScan.asStateFlow()

    init {
        checkBackendHealth()
        preSeedSampleCarsIfEmpty()
    }

    private fun preSeedSampleCarsIfEmpty() {
        viewModelScope.launch {
            val existing = allScans.value
            if (existing.isEmpty()) {
                val context = getApplication<Application>()
                com.example.data.SampleVehicleProvider.sampleCars.forEachIndexed { index, car ->
                    val file = com.example.data.SampleVehicleProvider.getOrCreateSampleImageFile(context, car)
                    val uri = Uri.fromFile(file).toString()
                    val vehicle = com.example.domain.model.Vehicle(
                        vehicleId = 1,
                        boundingBox = com.example.domain.model.BoundingBox(0.20f, 0.08f, 0.82f, 0.94f),
                        detectionConfidence = car.detectionConfidence,
                        make = car.make,
                        model = car.model,
                        classificationConfidence = car.classificationConfidence,
                        colour = car.colour,
                        colourHex = car.colourHex,
                        status = "identified",
                        topCandidates = car.topCandidates
                    )
                    repository.saveScan(
                        imageUri = uri,
                        vehicle = vehicle,
                        vehicleCount = 1
                    )
                }
            }
        }
    }

    fun loadSampleCar(car: com.example.data.PreIdentifiedCar) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val file = com.example.data.SampleVehicleProvider.getOrCreateSampleImageFile(context, car)
            val uri = Uri.fromFile(file)
            _selectedImageFile.value = file
            _selectedImageUri.value = uri
            val result = com.example.data.SampleVehicleProvider.toAnalysisResult(car, uri.toString())
            _analysisState.value = AnalysisUiState.Success(result)
            _selectedVehicleId.value = 1
            _isSavedCurrentScan.value = true
        }
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            _isBackendOnline.value = repository.checkBackendHealth()
        }
    }

    fun updateServerUrl(newUrl: String) {
        settingsRepository.updateServerUrl(newUrl)
        checkBackendHealth()
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _selectedImageUri.value = uri
            _analysisState.value = AnalysisUiState.Idle
            _isSavedCurrentScan.value = false

            // Copy to local app storage
            val localFile = copyUriToInternalStorage(uri)
            _selectedImageFile.value = localFile
        }
    }

    fun selectVehicle(vehicleId: Int) {
        _selectedVehicleId.value = vehicleId
    }

    fun startAnalysis() {
        val file = _selectedImageFile.value
        val uri = _selectedImageUri.value
        if (file == null || uri == null) {
            _analysisState.value = AnalysisUiState.Error("No image selected to analyze.")
            return
        }

        viewModelScope.launch {
            _isSavedCurrentScan.value = false
            repository.analyzeVehicleImage(file, uri.toString()).collect { progress ->
                when (progress) {
                    is AnalysisProgress.InProgress -> {
                        _analysisState.value = AnalysisUiState.Loading(progress.stage)
                    }
                    is AnalysisProgress.Success -> {
                        _analysisState.value = AnalysisUiState.Success(progress.result)
                        _selectedVehicleId.value = progress.result.vehicles.firstOrNull()?.vehicleId ?: 1
                    }
                    is AnalysisProgress.Error -> {
                        _analysisState.value = AnalysisUiState.Error(progress.message, progress.canFallback)
                    }
                }
            }
        }
    }

    fun runOfflineDemoAnalysis() {
        val file = _selectedImageFile.value
        val uri = _selectedImageUri.value
        if (file == null || uri == null) return

        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Loading(AnalysisStage.ANALYZING_COLOUR)
            val demoResult = repository.runOnDeviceDemoAnalysis(file, uri.toString())
            _analysisState.value = AnalysisUiState.Success(demoResult)
            _selectedVehicleId.value = 1
        }
    }

    fun saveCurrentScan(selectedVehicle: Vehicle) {
        val state = _analysisState.value
        if (state !is AnalysisUiState.Success) return

        viewModelScope.launch {
            val uri = _selectedImageUri.value?.toString() ?: ""
            repository.saveScan(
                imageUri = uri,
                vehicle = selectedVehicle,
                vehicleCount = state.result.vehicles.size,
                annotatedImageBase64 = state.result.annotatedImageBase64
            )
            _isSavedCurrentScan.value = true
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetAnalysis() {
        _analysisState.value = AnalysisUiState.Idle
        _selectedImageUri.value = null
        _selectedImageFile.value = null
        _isSavedCurrentScan.value = false
    }

    private suspend fun copyUriToInternalStorage(uri: Uri): File = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val fileName = "vehicle_scan_${System.currentTimeMillis()}.jpg"
        val destFile = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
            FileOutputStream(destFile).use { output: FileOutputStream ->
                input.copyTo(output)
            }
        }
        destFile
    }
}
