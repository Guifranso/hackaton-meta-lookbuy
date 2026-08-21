package com.lookbuy.app.ai_engine.speech

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Cliente de síntese de fala (TTS) para o pipeline LookBuy.
 *
 * Roteado para o dispositivo HFP ativo via [AudioAttributes.USAGE_VOICE_COMMUNICATION].
 * Isso garante que o áudio vá para o fone BT configurado pelo [DatAudioClient],
 * e não para o alto-falante do celular.
 *
 * Linguagem padrão: PT-BR.
 */
class TtsClient(private val context: Context) {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /**
     * Inicializa o motor TTS. Deve ser chamado antes de [speak].
     * A inicialização é assíncrona — aguarde [isReady] = true.
     */
    fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("pt", "BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "PT-BR indisponível, usando locale padrão.")
                    tts?.language = Locale.getDefault()
                }
                // Rotear para o dispositivo HFP ativo (fone BT / óculos)
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        Log.e(TAG, "Erro TTS: $utteranceId")
                    }
                })
                _isReady.value = true
                Log.d(TAG, "TtsClient pronto (PT-BR, roteado para HFP).")
            } else {
                Log.e(TAG, "Falha ao inicializar TTS: status=$status")
            }
        }
    }

    /**
     * Sintetiza e reproduz [text] no dispositivo HFP ativo.
     * Interrompe qualquer fala anterior (QUEUE_FLUSH).
     */
    fun speak(text: String) {
        if (!_isReady.value) {
            Log.w(TAG, "TTS ainda não pronto.")
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Log.d(TAG, "TTS: \"$text\"")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        Log.d(TAG, "TtsClient destruído.")
    }

    companion object {
        private const val TAG = "TtsClient"
    }
}
