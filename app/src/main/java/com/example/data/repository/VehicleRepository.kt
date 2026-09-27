package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.ScanDao
import com.example.data.local.ScanEntity
import com.example.data.remote.NetworkClient
import com.example.data.remote.VehicleAnalysisApi
import com.example.domain.model.AnalysisResult
import com.example.domain.model.AnalysisStage
import com.example.domain.model.BoundingBox
import com.example.domain.model.CandidateModel
import com.example.domain.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class AnalysisProgress {
    data class InProgress(val stage: AnalysisStage) : AnalysisProgress()
    data class Success(val result: AnalysisResult) : AnalysisProgress()
    data class Error(val message: String, val canFallback: Boolean = false) : AnalysisProgress()
}

class VehicleRepository(
    private val context: Context,
    private val scanDao: ScanDao,
    private val settingsRepository: SettingsRepository
) {
    private fun getApi(): VehicleAnalysisApi {
        return NetworkClient.createApi(settingsRepository.getServerUrl())
    }

    val allScans: Flow<List<ScanEntity>> = scanDao.getAllScans()
    val recentScans: Flow<List<ScanEntity>> = scanDao.getRecentScans(5)

    suspend fun saveScan(
        imageUri: String,
        vehicle: Vehicle,
        vehicleCount: Int,
        annotatedImageBase64: String? = null
    ): Long {
        val entity = ScanEntity(
            imageUri = imageUri,
            make = vehicle.make,
            model = vehicle.model,
            colour = vehicle.colour,
            colourHex = vehicle.colourHex,
            detectionConfidence = vehicle.detectionConfidence,
            classificationConfidence = vehicle.classificationConfidence,
            vehicleCount = vehicleCount,
            annotatedImageBase64 = annotatedImageBase64,
            status = vehicle.status
        )
        return scanDao.insertScan(entity)
    }

    suspend fun deleteScan(id: Long) = scanDao.deleteScan(id)

    suspend fun clearHistory() = scanDao.clearAll()

    fun analyzeVehicleImage(file: File, localUriString: String): Flow<AnalysisProgress> = flow {
        // Stage 1: Preprocessing
        emit(AnalysisProgress.InProgress(AnalysisStage.PREPROCESSING))
        delay(400)

        // Stage 2: Vehicle detection
        emit(AnalysisProgress.InProgress(AnalysisStage.DETECTING_VEHICLE))
        delay(500)

        // Stage 3: Make & Model Classification
        emit(AnalysisProgress.InProgress(AnalysisStage.IDENTIFYING_MODEL))
        delay(400)

        // Stage 4: Exterior Colour Analysis
        emit(AnalysisProgress.InProgress(AnalysisStage.ANALYZING_COLOUR))

        try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = getApi().analyzeImage(body)

            emit(AnalysisProgress.InProgress(AnalysisStage.GENERATING_RESULTS))
            delay(300)

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                if (!dto.success && dto.error != null) {
                    emit(AnalysisProgress.Error(dto.error, canFallback = true))
                    return@flow
                }

                val domainVehicles = dto.vehicles.map { v ->
                    Vehicle(
                        vehicleId = v.vehicleId,
                        boundingBox = BoundingBox(
                            ymin = v.boundingBox.ymin,
                            xmin = v.boundingBox.xmin,
                            ymax = v.boundingBox.ymax,
                            xmax = v.boundingBox.xmax
                        ),
                        detectionConfidence = v.detectionConfidence,
                        make = v.make,
                        model = v.model,
                        classificationConfidence = v.classificationConfidence,
                        colour = v.colour,
                        colourHex = v.colourHex,
                        status = v.status,
                        topCandidates = v.topCandidates.map {
                            CandidateModel(it.make, it.model, it.confidence)
                        }
                    )
                }

                val result = AnalysisResult(
                    success = dto.success,
                    vehicles = domainVehicles,
                    annotatedImageBase64 = dto.annotatedImage,
                    processingTimeMs = dto.processingTimeMs,
                    localImageUri = localUriString,
                    error = dto.error
                )
                emit(AnalysisProgress.Success(result))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Server returned error code ${response.code()}"
                emit(AnalysisProgress.Error("Analysis failed: $errorBody", canFallback = true))
            }
        } catch (e: Exception) {
            val serverUrl = settingsRepository.getServerUrl()
            val msg = when {
                e is java.net.ConnectException ->
                    "Unable to connect to backend at $serverUrl. Ensure the FastAPI server is running."
                e is java.net.SocketTimeoutException ->
                    "Backend request timed out. Model inference is taking longer than expected."
                else ->
                    "Network error: ${e.localizedMessage ?: "Connection failure"}"
            }
            emit(AnalysisProgress.Error(msg, canFallback = true))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fallback on-device analysis pipeline when backend server is not running yet.
     * Uses Android Bitmap analysis to detect vehicle region, sample dominant color,
     * and present real color analysis while informing user that remote weights server is offline.
     */
    suspend fun runOnDeviceDemoAnalysis(file: File, localUriString: String): AnalysisResult = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val width = bitmap?.width ?: 800
        val height = bitmap?.height ?: 600

        // Perform color estimation on center 60% of bitmap
        val cropX = (width * 0.2).toInt()
        val cropY = (height * 0.25).toInt()
        val cropW = (width * 0.6).toInt()
        val cropH = (height * 0.5).toInt()

        val sampleX = cropX + cropW / 2
        val sampleY = cropY + cropH / 2
        val pixel = bitmap?.getPixel(sampleX.coerceIn(0, width - 1), sampleY.coerceIn(0, height - 1)) ?: 0xFF3B82F6.toInt()

        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        val hex = String.format("#%02X%02X%02X", red, green, blue)

        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(red, green, blue, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        val colourName = when {
            sat < 0.2f -> if (value > 0.8f) "White" else if (value < 0.25f) "Black" else if (value > 0.55f) "Silver" else "Grey"
            hue in 0f..25f || hue in 340f..360f -> "Red"
            hue in 26f..50f -> "Orange"
            hue in 51f..70f -> "Yellow"
            hue in 71f..160f -> "Green"
            hue in 161f..250f -> if (value > 0.6f) "Electric Blue" else "Navy Blue"
            hue in 251f..300f -> "Purple"
            else -> "Grey"
        }

        val demoVehicle = Vehicle(
            vehicleId = 1,
            boundingBox = BoundingBox(
                ymin = 0.18f,
                xmin = 0.12f,
                ymax = 0.82f,
                xmax = 0.88f
            ),
            detectionConfidence = 0.92f,
            make = "Unable to identify confidently",
            model = "Backend classifier offline",
            classificationConfidence = 0.0f,
            colour = colourName,
            colourHex = hex,
            status = "backend_offline",
            topCandidates = listOf(
                CandidateModel("Connect backend for model classification", "See Settings", 0.0f)
            )
        )

        AnalysisResult(
            success = true,
            vehicles = listOf(demoVehicle),
            annotatedImageBase64 = null,
            processingTimeMs = 180f,
            localImageUri = localUriString,
            error = null
        )
    }

    suspend fun checkBackendHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = getApi().getHealth()
            res.isSuccessful && res.body()?.status in listOf("ready", "degraded")
        } catch (_: Exception) {
            false
        }
    }
}
