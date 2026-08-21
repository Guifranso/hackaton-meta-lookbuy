package com.lookbuy.app.meta_wearables.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HfpState {
    DISCONNECTED, CONNECTING, ACTIVE
}

/**
 * Gerencia o roteamento de áudio via HFP (Hands-Free Profile) para os
 * fones Bluetooth (ou óculos Ray-Ban Meta em hardware real).
 *
 * Arquitetura §5, item 8:
 * O DAT SDK NÃO gerencia áudio. O app deve configurar o HFP via APIs
 * padrão do Android ANTES de iniciar o streaming de vídeo.
 *
 * Para testar sem os óculos: par qualquer fone Bluetooth com perfil HFP
 * (ex: Apple EarPods Bluetooth, Samsung Galaxy Buds) ao celular.
 * O app não distingue entre o fone e os óculos — ambos aparecem como SCO.
 */
class DatAudioClient(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _hfpState = MutableStateFlow(HfpState.DISCONNECTED)
    val hfpState: StateFlow<HfpState> = _hfpState.asStateFlow()

    private var scoReceiver: BroadcastReceiver? = null

    /**
     * Ativa o modo HFP.
     *
     * Em API 31+: usa [AudioManager.setCommunicationDevice] com o dispositivo SCO.
     * Em API 29–30: usa [AudioManager.startBluetoothSco] + [AudioManager.MODE_IN_COMMUNICATION].
     *
     * Chame isso ANTES de iniciar a sessão DAT.
     */
    fun startHfp() {
        if (_hfpState.value == HfpState.ACTIVE) return
        Log.d(TAG, "Ativando HFP...")
        _hfpState.value = HfpState.CONNECTING

        registerScoReceiver()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: setCommunicationDevice é o método preferido
            val scoDevice = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            if (scoDevice != null) {
                val success = audioManager.setCommunicationDevice(scoDevice)
                if (success) {
                    _hfpState.value = HfpState.ACTIVE
                    Log.d(TAG, "HFP ativo via setCommunicationDevice (API 31+).")
                    return
                }
            }
            Log.w(TAG, "setCommunicationDevice indisponível — fallback para startBluetoothSco.")
        }

        // Fallback para API 29-30 (ou quando SCO device não está disponível ainda)
        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        Log.d(TAG, "startBluetoothSco() chamado. Aguardando SCO_AUDIO_STATE_CONNECTED...")
    }

    /**
     * Desativa o roteamento HFP e restaura o modo de áudio padrão.
     */
    fun stopHfp() {
        Log.d(TAG, "Desativando HFP.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        @Suppress("DEPRECATION")
        audioManager.stopBluetoothSco()
        audioManager.mode = AudioManager.MODE_NORMAL
        unregisterScoReceiver()
        _hfpState.value = HfpState.DISCONNECTED
    }

    // ── SCO Broadcast Receiver ────────────────────────────────────────────────

    private fun registerScoReceiver() {
        if (scoReceiver != null) return
        scoReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                Log.d(TAG, "SCO state: $state")
                when (state) {
                    AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                        _hfpState.value = HfpState.ACTIVE
                        Log.d(TAG, "SCO conectado — HFP ativo.")
                    }
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                        if (_hfpState.value != HfpState.DISCONNECTED) {
                            _hfpState.value = HfpState.DISCONNECTED
                        }
                        Log.d(TAG, "SCO desconectado.")
                    }
                    AudioManager.SCO_AUDIO_STATE_CONNECTING -> {
                        _hfpState.value = HfpState.CONNECTING
                    }
                }
            }
        }
        context.registerReceiver(
            scoReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED),
        )
        Log.d(TAG, "SCO BroadcastReceiver registrado.")
    }

    private fun unregisterScoReceiver() {
        scoReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        scoReceiver = null
    }

    companion object {
        private const val TAG = "DatAudioClient"
    }
}
