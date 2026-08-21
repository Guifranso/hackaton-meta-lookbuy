package com.lookbuy.app.ai_engine.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cliente de reconhecimento de fala (STT) para o pipeline LookBuy.
 *
 * Captura de áudio: O microfone ativo é o dispositivo de comunicação configurado
 * pelo [DatAudioClient]. O Android roteia automaticamente para o fone BT HFP.
 *
 * Implementação atual: [SpeechRecognizer] do Android (online, PT-BR).
 * TODO Fase 3/4: substituir por vosk-android para reconhecimento offline.
 *
 * IMPORTANTE: [initialize] e [startListening] devem ser chamados na thread principal.
 */
class SttClient(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    /** Resultado parcial em tempo real (exibido na UI enquanto fala). */
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    /** Resultado final emitido após o silêncio detectado. */
    private val _recognizedText = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val recognizedText: SharedFlow<String> = _recognizedText.asSharedFlow()

    /** Último erro de reconhecimento (código Android). */
    private val _lastError = MutableStateFlow<Int?>(null)
    val lastError: StateFlow<Int?> = _lastError.asStateFlow()

    fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer não disponível neste dispositivo.")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(listener)
        Log.d(TAG, "SttClient inicializado.")
    }

    fun startListening() {
        if (_isListening.value) return
        _lastError.value = null
        _partialText.value = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
        _isListening.value = true
        Log.d(TAG, "Escutando...")
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "SttClient destruído.")
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Pronto para fala.")
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            _partialText.value = partial
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            _partialText.value = ""
            _isListening.value = false
            Log.d(TAG, "Reconhecido: \"$text\"")
            _recognizedText.tryEmit(text)
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _partialText.value = ""
            _lastError.value = error
            Log.e(TAG, "Erro STT (código $error): ${errorMessage(error)}")
        }

        override fun onEndOfSpeech() { _isListening.value = false }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        private const val TAG = "SttClient"

        fun errorMessage(code: Int): String = when (code) {
            SpeechRecognizer.ERROR_AUDIO              -> "Erro de gravação de áudio"
            SpeechRecognizer.ERROR_CLIENT             -> "Erro do cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem permissão RECORD_AUDIO"
            SpeechRecognizer.ERROR_NETWORK            -> "Erro de rede"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT    -> "Timeout de rede"
            SpeechRecognizer.ERROR_NO_MATCH           -> "Nenhuma correspondência encontrada"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY    -> "Reconhecedor ocupado"
            SpeechRecognizer.ERROR_SERVER             -> "Erro no servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT     -> "Timeout de fala"
            else                                      -> "Erro desconhecido ($code)"
        }
    }
}
