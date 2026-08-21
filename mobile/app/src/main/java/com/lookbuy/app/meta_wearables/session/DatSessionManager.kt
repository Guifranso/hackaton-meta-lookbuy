package com.lookbuy.app.meta_wearables.session

import android.app.Activity
import android.content.Context
import android.util.Log
import com.lookbuy.app.BuildConfig
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.mockdevice.MockDeviceKit
import com.meta.wearable.dat.mockdevice.api.GlassesModel
import com.meta.wearable.dat.mockdevice.api.MockGlasses
import com.meta.wearable.dat.mockdevice.api.camera.CameraFacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gerencia o ciclo de vida completo da sessão com o Meta DAT SDK.
 *
 * Responsabilidades:
 * - Habilitar o [MockDeviceKit] em builds DEBUG, simulando os óculos Ray-Ban Meta
 *   com câmera traseira do celular (Arquitetura §2 e §5, itens 1–5).
 * - Orquestrar o fluxo de Registro → Sessão conforme as regras do SDK.
 * - Expor [StateFlow]s de registro e sessão para a camada de apresentação.
 *
 * Pacotes reais do SDK (inspecionados do JAR):
 *   com.meta.wearable.dat.core.Wearables        → singleton INSTANCE
 *   com.meta.wearable.dat.core.types.RegistrationState
 *   com.meta.wearable.dat.core.session.DeviceSession / DeviceSessionState
 *   com.meta.wearable.dat.mockdevice.MockDeviceKit
 *   com.meta.wearable.dat.mockdevice.api.GlassesModel / MockGlasses
 */
class DatSessionManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ── Estado de Registro ────────────────────────────────────────────────────
    private val _registrationState = MutableStateFlow(RegistrationState.UNAVAILABLE)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    // ── Estado da Sessão ──────────────────────────────────────────────────────
    private val _sessionState = MutableStateFlow(DeviceSessionState.IDLE)
    val sessionState: StateFlow<DeviceSessionState> = _sessionState.asStateFlow()

    private val _deviceSession = MutableStateFlow<DeviceSession?>(null)
    val deviceSession: StateFlow<DeviceSession?> = _deviceSession.asStateFlow()

    // ── MockDeviceKit (apenas DEBUG) ──────────────────────────────────────────

    /**
     * Ativa o Mock Device Kit e simula os óculos em estado de uso.
     *
     * Arquitetura §2:
     * - [MockDeviceKit.enable] ativa o simulador.
     * - [pairGlasses] retorna um [MockGlasses]; depois chamamos [powerOn] e [don].
     *
     * Comportamento documentado: imagem retorna rotacionada 90° — será
     * corrigido na Fase 3 pelo DatCameraClient.
     */
    suspend fun setupMockDevice() {
        if (!BuildConfig.DEBUG) return
        try {
            val mockKit = MockDeviceKit.Companion.getInstance(context)
            mockKit.enable()

            // pairGlasses retorna DatResult<MockGlasses, ...> — usar .getOrNull(), NÃO cast direto
            // Referência: Arquitetura §5.1
            val mockGlasses = mockKit.pairGlasses(GlassesModel.RAYBAN_META).getOrNull()
            if (mockGlasses == null) {
                Log.e(TAG, "pairGlasses falhou — MockDeviceKit pode não estar inicializado corretamente.")
                return
            }

            mockGlasses.powerOn()
            mockGlasses.unfold()
            mockGlasses.don()

            // Configura câmera traseira do celular como feed do MDK (Arquitetura §5.1)
            // A imagem retorna rotacionada 90° — corrigido no DatCameraClient.correctMdkRotation()
            mockGlasses.services.camera.setCameraFeed(CameraFacing.BACK)

            Log.d(TAG, "MockDeviceKit ativo: Ray-Ban Meta simulado (powerOn + unfold + don + câmera traseira).")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao configurar MockDeviceKit: ${e.message}", e)
        }
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    /**
     * Inicia o fluxo de registro no Meta AI e observa o estado até [RegistrationState.REGISTERED].
     *
     * Arquitetura §5, item 4:
     * Não é possível criar sessão antes do registro. O intent-filter com
     * URI scheme "lookbuy://" no Manifest é o que permite o retorno do app Meta AI.
     */
    fun startRegistration(activity: Activity) {
        scope.launch {
            Wearables.registrationState.collect { state ->
                _registrationState.value = state
                Log.d(TAG, "Estado de registro: $state")
                if (state == RegistrationState.REGISTERED && _deviceSession.value == null) {
                    startSession()
                }
            }
        }
        Wearables.startRegistration(activity)
        Log.d(TAG, "Registro iniciado.")
    }

    // ── Sessão ────────────────────────────────────────────────────────────────

    /** Último erro de sessão, exposto para diagnóstico na UI. */
    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError.asStateFlow()

    /**
     * Cria e inicia a [DeviceSession] após o registro ser confirmado.
     *
     * Arquitetura §5, item 5:
     * start()/stop() são fire-and-forget. A UI deve observar [sessionState]
     * e aguardar [DeviceSessionState.STARTED] antes de iniciar streams de vídeo.
     */
    private fun startSession() {
        scope.launch {
            try {
                Log.d(TAG, "Criando sessão DAT com AutoDeviceSelector...")
                // AutoDeviceSelector() seleciona qualquer dispositivo pareado (incluindo mock)
                val selector = com.meta.wearable.dat.core.selectors.AutoDeviceSelector()
                // createSession retorna DatResult<DeviceSession, DeviceSessionError>
                val result = Wearables.createSession(selector)
                val session = result.getOrNull()
                if (session == null) {
                    val err = "createSession falhou — verifique se o MockDeviceKit está ativo. Detalhe: $result"
                    Log.e(TAG, err)
                    _sessionError.value = err
                    return@launch
                }
                _sessionError.value = null
                _deviceSession.value = session
                launch {
                    session.state.collect { state ->
                        _sessionState.value = state
                        Log.d(TAG, "Estado da sessão: $state")
                    }
                }
                session.start()
                Log.d(TAG, "Sessão iniciada (fire-and-forget). Aguardando STARTED...")
            } catch (e: Exception) {
                val err = "Exceção ao criar sessão: ${e.message}"
                Log.e(TAG, err, e)
                _sessionError.value = err
            }
        }
    }

    fun stopSession() {
        _deviceSession.value?.stop()
        _deviceSession.value = null
        _sessionState.value = DeviceSessionState.IDLE
        Log.d(TAG, "Sessão encerrada.")
    }


    // ── Singleton ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "DatSessionManager"

        @Volatile
        private var instance: DatSessionManager? = null

        fun getInstance(context: Context): DatSessionManager =
            instance ?: synchronized(this) {
                instance ?: DatSessionManager(context.applicationContext).also { instance = it }
            }
    }
}
