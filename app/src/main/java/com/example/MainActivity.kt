package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ServerConfigDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyBackground
import com.example.viewmodel.AnalysisUiState
import com.example.viewmodel.AutoVisionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyBackground
                ) {
                    AutoVisionApp()
                }
            }
        }
    }
}

@Composable
fun AutoVisionApp(
    viewModel: AutoVisionViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var showServerSettings by remember { mutableStateOf(false) }

    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val selectedImageFile by viewModel.selectedImageFile.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSavedCurrentScan.collectAsStateWithLifecycle()
    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
    val allScans by viewModel.allScans.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val isBackendOnline by viewModel.isBackendOnline.collectAsStateWithLifecycle()

    // Handle back button for non-home screens
    if (currentScreen != Screen.HOME && currentScreen != Screen.SPLASH) {
        BackHandler {
            when (currentScreen) {
                Screen.PREVIEW -> {
                    viewModel.resetAnalysis()
                    currentScreen = Screen.HOME
                }
                Screen.ANALYSIS -> {
                    viewModel.resetAnalysis()
                    currentScreen = Screen.PREVIEW
                }
                Screen.RESULTS -> {
                    viewModel.resetAnalysis()
                    currentScreen = Screen.HOME
                }
                Screen.HISTORY -> currentScreen = Screen.HOME
                Screen.ABOUT -> currentScreen = Screen.HOME
                else -> currentScreen = Screen.HOME
            }
        }
    }

    // Auto-advance to Results screen when analysis succeeds
    LaunchedEffect(analysisState) {
        if (analysisState is AnalysisUiState.Success && currentScreen == Screen.ANALYSIS) {
            currentScreen = Screen.RESULTS
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { targetScreen ->
        when (targetScreen) {
            Screen.SPLASH -> {
                SplashScreen(
                    onSplashComplete = {
                        currentScreen = Screen.HOME
                    }
                )
            }

            Screen.HOME -> {
                HomeScreen(
                    recentScans = recentScans,
                    isBackendOnline = isBackendOnline,
                    onImageSelected = { uri ->
                        viewModel.onImageSelected(uri)
                        currentScreen = Screen.PREVIEW
                    },
                    onSelectSampleCar = { car ->
                        viewModel.loadSampleCar(car)
                        currentScreen = Screen.RESULTS
                    },
                    onNavigateToHistory = {
                        currentScreen = Screen.HISTORY
                    },
                    onNavigateToAbout = {
                        currentScreen = Screen.ABOUT
                    },
                    onOpenSettings = {
                        showServerSettings = true
                    }
                )
            }

            Screen.PREVIEW -> {
                if (selectedImageUri != null) {
                    PreviewScreen(
                        imageUri = selectedImageUri!!,
                        imageFile = selectedImageFile,
                        onAnalyzeClick = {
                            viewModel.startAnalysis()
                            currentScreen = Screen.ANALYSIS
                        },
                        onReplaceImage = { uri ->
                            viewModel.onImageSelected(uri)
                        },
                        onCancel = {
                            viewModel.resetAnalysis()
                            currentScreen = Screen.HOME
                        }
                    )
                } else {
                    currentScreen = Screen.HOME
                }
            }

            Screen.ANALYSIS -> {
                if (selectedImageUri != null) {
                    AnalysisScreen(
                        imageUri = selectedImageUri!!,
                        analysisState = analysisState,
                        onRetry = {
                            viewModel.startAnalysis()
                        },
                        onRunFallbackDemo = {
                            viewModel.runOfflineDemoAnalysis()
                        },
                        onOpenSettings = {
                            showServerSettings = true
                        },
                        onCancel = {
                            viewModel.resetAnalysis()
                            currentScreen = Screen.HOME
                        }
                    )
                } else {
                    currentScreen = Screen.HOME
                }
            }

            Screen.RESULTS -> {
                val state = analysisState
                if (state is AnalysisUiState.Success) {
                    ResultsScreen(
                        result = state.result,
                        selectedVehicleId = selectedVehicleId,
                        isSaved = isSaved,
                        onSelectVehicle = { id ->
                            viewModel.selectVehicle(id)
                        },
                        onSaveClick = { vehicle ->
                            viewModel.saveCurrentScan(vehicle)
                        },
                        onAnalyzeAnother = {
                            viewModel.resetAnalysis()
                            currentScreen = Screen.HOME
                        },
                        onBack = {
                            viewModel.resetAnalysis()
                            currentScreen = Screen.HOME
                        }
                    )
                } else {
                    currentScreen = Screen.HOME
                }
            }

            Screen.HISTORY -> {
                HistoryScreen(
                    scans = allScans,
                    onDeleteScan = { id -> viewModel.deleteScan(id) },
                    onClearAll = { viewModel.clearAllHistory() },
                    onBack = { currentScreen = Screen.HOME }
                )
            }

            Screen.ABOUT -> {
                AboutScreen(
                    onBack = { currentScreen = Screen.HOME }
                )
            }
        }
    }

    if (showServerSettings) {
        ServerConfigDialog(
            currentUrl = serverUrl,
            onSaveUrl = { newUrl ->
                viewModel.updateServerUrl(newUrl)
            },
            onDismiss = {
                showServerSettings = false
            }
        )
    }
}
