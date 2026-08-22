package com.lookbuy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lookbuy.app.presentation.screens.MainScreen
import com.lookbuy.app.presentation.viewmodels.LookBuyViewModel
import com.lookbuy.app.ui.theme.LookBuyTheme

import android.os.Build

class MainActivity : ComponentActivity() {

    private val viewModel: LookBuyViewModel by viewModels()

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions.toTypedArray()
    }

    // Solicitação de permissões necessárias (Áudio, Câmera e Bluetooth)
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.initialize(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = getRequiredPermissions()
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.initialize(this)
        } else {
            requestPermissionsLauncher.launch(permissions)
        }

        setContent {
            LookBuyTheme {
                val registrationState  by viewModel.registrationState.collectAsStateWithLifecycle()
                val sessionState       by viewModel.sessionState.collectAsStateWithLifecycle()
                val sessionError       by viewModel.sessionError.collectAsStateWithLifecycle()
                val streamState        by viewModel.streamState.collectAsStateWithLifecycle()
                val hfpState           by viewModel.hfpState.collectAsStateWithLifecycle()
                val isListening        by viewModel.isListening.collectAsStateWithLifecycle()
                val isSpeaking         by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
                val partialText        by viewModel.partialText.collectAsStateWithLifecycle()
                val lastRecognizedText by viewModel.lastRecognizedText.collectAsStateWithLifecycle()
                val lastCapturedPhoto  by viewModel.lastCapturedPhoto.collectAsStateWithLifecycle()
                val cameraPreview      by viewModel.cameraPreview.collectAsStateWithLifecycle()
                val isProcessing       by viewModel.isProcessing.collectAsStateWithLifecycle()
                val assistantState     by viewModel.assistantState.collectAsStateWithLifecycle()
                val lastAnalysisResult by viewModel.lastAnalysisResult.collectAsStateWithLifecycle()

                MainScreen(
                    registrationState  = registrationState,
                    sessionState       = sessionState,
                    sessionError       = sessionError,
                    streamState        = streamState,
                    hfpState           = hfpState,
                    isListening        = isListening,
                    isSpeaking         = isSpeaking,
                    isProcessing       = isProcessing,
                    partialText        = partialText,
                    lastRecognizedText = lastRecognizedText,
                    lastCapturedPhoto  = lastCapturedPhoto,
                    cameraPreview      = cameraPreview,
                    lastAnalysisResult = lastAnalysisResult,
                    assistantState     = assistantState,
                    onMicPress         = viewModel::startListening,
                    onMicRelease       = viewModel::stopListening,
                    onActivateAssistant = viewModel::activateAssistant,
                    onDeactivateAssistant = viewModel::deactivateAssistant,
                )
            }
        }
    }
}
