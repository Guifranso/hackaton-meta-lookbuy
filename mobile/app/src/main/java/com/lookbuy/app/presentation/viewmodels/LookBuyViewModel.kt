package com.lookbuy.app.presentation.viewmodels

import android.app.Application
import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lookbuy.app.ai_engine.speech.SttClient
import com.lookbuy.app.ai_engine.speech.TtsClient
import com.lookbuy.app.ai_engine.vision.ProductAnalysisResult
import com.lookbuy.app.ai_engine.vision.VisionClient
import com.lookbuy.app.domain.usecases.ProcessProductLookUseCase
import com.lookbuy.app.meta_wearables.audio.DatAudioClient
import com.lookbuy.app.meta_wearables.audio.HfpState
import com.lookbuy.app.meta_wearables.camera.DatCameraClient
import com.lookbuy.app.meta_wearables.session.DatSessionManager
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.RegistrationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel principal do LookBuy.
 *
 * Orquestra:
 * 1. MockDeviceKit (DEBUG) com mock de câmera.
 * 2. HFP via [DatAudioClient] — ANTES de iniciar o streaming de vídeo.
 * 3. Registro + Sessão DAT via [DatSessionManager].
 * 4. Câmera ([DatCameraClient]) — anexa o stream automaticamente quando a sessão fica STARTED.
 * 5. STT ([SttClient]) e TTS ([TtsClient]) para o pipeline de voz.
 * 6. VLM ([VisionClient]) orquestrado por [ProcessProductLookUseCase].
 */
class LookBuyViewModel(application: Application) : AndroidViewModel(application) {

    // ── Clientes ──────────────────────────────────────────────────────────────
    private val sessionManager = DatSessionManager.getInstance(application)
    private val audioClient    = DatAudioClient(application)
    private val cameraClient   = DatCameraClient(application)
    private val sttClient      = SttClient(application)
    private val ttsClient      = TtsClient(application)
    
    private val visionClient   = VisionClient()
    private val processLookUseCase = ProcessProductLookUseCase(visionClient, ttsClient)

    // ── Estados DAT ───────────────────────────────────────────────────────────

    val registrationState: StateFlow<RegistrationState> = sessionManager.registrationState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrationState.UNAVAILABLE)

    val sessionState: StateFlow<DeviceSessionState> = sessionManager.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceSessionState.IDLE)

    /** Erro de criação de sessão (null = sem erro). Exibido nos StatusCards para diagnóstico. */
    val sessionError: StateFlow<String?> = sessionManager.sessionError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val streamState: StateFlow<StreamState> = cameraClient.streamState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreamState.STOPPED)

    // ── Estados de Áudio e Captura ────────────────────────────────────────────

    val hfpState: StateFlow<HfpState> = audioClient.hfpState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HfpState.DISCONNECTED)

    val isListening: StateFlow<Boolean> = sttClient.isListening
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val partialText: StateFlow<String> = sttClient.partialText
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val isTtsSpeaking: StateFlow<Boolean> = ttsClient.isSpeaking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _lastCapturedPhoto = MutableStateFlow<Bitmap?>(null)
    val lastCapturedPhoto: StateFlow<Bitmap?> = _lastCapturedPhoto.asStateFlow()

    private val _cameraPreview = MutableStateFlow<Bitmap?>(null)
    val cameraPreview: StateFlow<Bitmap?> = _cameraPreview.asStateFlow()

    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastAnalysisResult = MutableStateFlow<ProductAnalysisResult?>(null)
    val lastAnalysisResult: StateFlow<ProductAnalysisResult?> = _lastAnalysisResult.asStateFlow()

    // ── Inicialização ─────────────────────────────────────────────────────────

    init {
        // Observa a sessão para anexar a câmera automaticamente quando iniciar
        combine(sessionManager.sessionState, sessionManager.deviceSession) { state, session ->
            Pair(state, session)
        }.filter { (state, session) ->
            state == DeviceSessionState.STARTED && session != null
        }.onEach { (_, session) ->
            if (cameraClient.streamState.value == StreamState.STOPPED) {
                Log.d("LookBuyViewModel", "Sessão iniciada, anexando stream de vídeo...")
                cameraClient.attachToSession(session!!)
            }
        }.launchIn(viewModelScope)

        // Observa erros de stream
        cameraClient.streamError
            .onEach { err -> Log.e("LookBuyViewModel", "Erro de stream: $err") }
            .launchIn(viewModelScope)

        // Recebe a foto capturada
        cameraClient.capturedPhoto
            .onEach { photo ->
                _lastCapturedPhoto.value = photo
                // Após a foto ser capturada, enviaremos pro VLM
                val speech = _lastRecognizedText.value
                if (speech.isNotEmpty()) {
                    runVisionPipeline(photo, speech)
                }
            }
            .launchIn(viewModelScope)

        // O DAT expõe frames YUV, mas não uma Surface de preview. Atualizamos uma
        // captura leve por segundo, mostrando na tela o mesmo feed que será analisado.
        cameraClient.previewPhoto
            .onEach { preview -> _cameraPreview.value = preview }
            .launchIn(viewModelScope)


        // Observa o resultado final do STT
        sttClient.recognizedText
            .onEach { text ->
                _lastRecognizedText.value = text
                
                // Limpa resultado anterior
                _lastAnalysisResult.value = null

                // Quando o usuário termina de falar, capturamos a foto
                if (cameraClient.streamState.value == StreamState.STREAMING) {
                    viewModelScope.launch {
                        _isProcessing.value = true
                        if (!cameraClient.capturePhoto()) {
                            // No MDK o stream e a foto de captura são configurados
                            // separadamente. Se a foto mock não existir, a prévia já
                            // contém um frame válido da câmera e pode ser analisada.
                            val preview = _cameraPreview.value
                            if (preview != null) {
                                Log.w("LookBuyViewModel", "capturePhoto falhou; analisando o último frame da prévia.")
                                _lastCapturedPhoto.value = preview
                                runVisionPipeline(preview, text)
                            } else {
                                _isProcessing.value = false
                                ttsClient.speak("Ainda não recebi uma imagem da câmera. Tente novamente em instantes.")
                            }
                        }
                    }
                } else {
                    Log.w("LookBuyViewModel", "Câmera não está em STREAMING, foto ignorada.")
                    ttsClient.speak("Câmera indisponível. Conecte o vídeo primeiro.")
                }
            }
            .launchIn(viewModelScope)
    }

    private fun runVisionPipeline(image: Bitmap, speechText: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                _lastAnalysisResult.value = processLookUseCase(image, speechText)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Inicializa todo o pipeline de áudio, mock de câmera e o SDK DAT.
     */
    fun initialize(activity: Activity) {
        // 1. Inicializa clientes de voz
        sttClient.initialize()
        ttsClient.initialize()

        // 2. Ativa HFP ANTES do streaming de vídeo
        audioClient.startHfp()

        // 3. Configura Mock de câmera e inicia o registro DAT
        viewModelScope.launch {
            sessionManager.setupMockDevice()
            cameraClient.setupMockCameraFeed()
            sessionManager.startRegistration(activity)
        }
    }

    // ── Controles de Voz ──────────────────────────────────────────────────────

    fun startListening() = sttClient.startListening()
    fun stopListening()  = sttClient.stopListening()

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        cameraClient.stop()
        sessionManager.stopSession()
        audioClient.stopHfp()
        sttClient.destroy()
        ttsClient.destroy()
    }
}
