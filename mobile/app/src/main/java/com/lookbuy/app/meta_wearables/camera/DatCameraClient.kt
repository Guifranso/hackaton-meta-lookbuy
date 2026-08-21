package com.lookbuy.app.meta_wearables.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Log
import com.lookbuy.app.BuildConfig
import com.meta.wearable.dat.camera.addStream
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.mockdevice.MockDeviceKit
import com.meta.wearable.dat.mockdevice.api.camera.CameraFacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Gerencia o stream de câmera do Meta DAT SDK.
 *
 * Responsabilidades (Arquitetura §5, itens 6 e 7):
 * - Adicionar o stream à sessão com [StreamConfiguration] MEDIUM + 15fps + compressVideo=false.
 * - Aguardar [StreamState.STREAMING] antes de capturar fotos.
 * - Corrigir a rotação de 90° gerada pelo MDK ao usar a câmera do celular.
 * - Expor frames e fotos para as camadas de IA (Fase 3) e visão (Fase 4).
 *
 * Ordem de uso:
 * 1. Aguardar [DeviceSessionState.STARTED] no [DatSessionManager].
 * 2. Chamar [attachToSession] passando a sessão ativa.
 * 3. Aguardar [streamState] == [StreamState.STREAMING].
 * 4. Chamar [capturePhoto] para obter um Bitmap pronto para inferência.
 */
class DatCameraClient(private val context: android.content.Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _streamState = MutableStateFlow(StreamState.STOPPED)
    val streamState: StateFlow<StreamState> = _streamState.asStateFlow()

    /** Emite fotos capturadas, já com rotação corrigida (MDK gira 90° no celular). */
    private val _capturedPhoto = MutableSharedFlow<Bitmap>(extraBufferCapacity = 4)
    val capturedPhoto: SharedFlow<Bitmap> = _capturedPhoto.asSharedFlow()

    /** Atualizações leves da prévia do feed para a interface de depuração. */
    private val _previewPhoto = MutableSharedFlow<Bitmap>(extraBufferCapacity = 1)
    val previewPhoto: SharedFlow<Bitmap> = _previewPhoto.asSharedFlow()

    /** Emite erros do stream para que o ViewModel possa reagir. */
    private val _streamError = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val streamError: SharedFlow<String> = _streamError.asSharedFlow()

    private var stream: com.meta.wearable.dat.camera.Stream? = null
    private var lastPreviewTimestampMs = 0L

    // ── Configuração do Feed MockDevice ───────────────────────────────────────

    /**
     * Configura a câmera traseira do celular como feed do MDK.
     *
     * Arquitetura §2: O MDK usa a câmera traseira (BACK) por padrão,
     * mas a imagem retorna rotacionada 90° — será corrigida em [correctMdkRotation].
     */
    fun setupMockCameraFeed() {
        if (!BuildConfig.DEBUG) return
        try {
            val mockKit = MockDeviceKit.Companion.getInstance(context)
            val pairedDevices = mockKit.pairedDevices
            val mockGlasses = pairedDevices
                .filterIsInstance<com.meta.wearable.dat.mockdevice.api.MockGlasses>()
                .firstOrNull() ?: return
            mockGlasses.services.camera.setCameraFeed(CameraFacing.BACK)
            Log.d(TAG, "MockCamera: câmera traseira configurada como feed.")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao configurar MockCamera: ${e.message}", e)
        }
    }

    // ── Stream de Vídeo ───────────────────────────────────────────────────────

    /**
     * Adiciona o stream de vídeo à [session] ativa.
     *
     * Arquitetura §5, item 5:
     * Só deve ser chamado quando [session.state] == [DeviceSessionState.STARTED].
     *
     * Configuração (Arquitetura §5, item 6):
     * - MEDIUM (504×896) a 15 fps, compressVideo=false → YUV ideal para modelos locais.
     */
    fun attachToSession(session: DeviceSession) {
        scope.launch {
            try {
                val config = StreamConfiguration(
                    videoQuality  = VideoQuality.MEDIUM,
                    frameRate     = 15,
                    compressVideo = false,
                )
                // addStream é suspend — retorna o Stream ou lança exceção
                val result = session.addStream(config)
                stream = result.getOrNull()
                if (stream == null) {
                    Log.e(TAG, "Falha ao adicionar stream: $result")
                    return@launch
                }
                Log.d(TAG, "Stream adicionado à sessão (MEDIUM, 15fps, YUV).")

                stream?.let { s ->
                    // Frames do DAT são YUV; convertemos somente dois por segundo para
                    // manter a prévia fluida o bastante sem competir com a análise.
                    launch {
                        s.videoStream.collect { frame ->
                            val now = SystemClock.elapsedRealtime()
                            if (!frame.isCompressed && now - lastPreviewTimestampMs >= 500) {
                                frameToBitmap(frame)?.let { _previewPhoto.tryEmit(it) }
                                lastPreviewTimestampMs = now
                            }
                        }
                    }
                    // Observa estado do stream
                    launch {
                        s.state.onEach { state ->
                            _streamState.value = state
                            Log.d(TAG, "StreamState: $state")
                        }.collect()
                    }
                    // Observa erros
                    launch {
                        s.errorStream.onEach { error ->
                            _streamError.tryEmit(error.toString())
                            Log.e(TAG, "Erro no stream: $error")
                        }.collect()
                    }
                    // Inicia o stream
                    s.start()
                    Log.d(TAG, "Stream iniciado. Aguardando STREAMING...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao adicionar stream à sessão: ${e.message}", e)
                _streamError.tryEmit(e.message ?: "Erro desconhecido no stream")
            }
        }
    }

    // ── Captura de Foto ───────────────────────────────────────────────────────

    /**
     * Captura uma foto e a emite via [capturedPhoto] após correção de rotação.
     *
     * Arquitetura §5, item 7:
     * - Só funciona quando [streamState] == [StreamState.STREAMING].
     * - O MDK retorna a imagem rotacionada 90° — aplicamos [correctMdkRotation].
     *
     * @return true se a captura foi iniciada com sucesso.
     */
    suspend fun capturePhoto(): Boolean {
        val currentStream = stream
        if (currentStream == null) {
            Log.w(TAG, "capturePhoto: stream nulo.")
            return false
        }
        if (_streamState.value != StreamState.STREAMING) {
            Log.w(TAG, "capturePhoto: stream não está em STREAMING (${_streamState.value}).")
            return false
        }
        return try {
            val result = currentStream.capturePhoto()
            val photo  = result.getOrNull()
            when (photo) {
                is PhotoData.Bitmap -> {
                    val corrected = correctMdkRotation(photo.bitmap)
                    _capturedPhoto.tryEmit(corrected)
                    Log.d(TAG, "Foto capturada: ${corrected.width}×${corrected.height}px.")
                    true
                }
                is PhotoData.HEIC -> {
                    Log.w(TAG, "Foto em formato HEIC — use PhotoData.Bitmap com compressVideo=false.")
                    false
                }
                null -> {
                    Log.e(TAG, "capturePhoto falhou: ${result}")
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao capturar foto: ${e.message}", e)
            false
        }
    }

    /**
     * Obtém uma imagem para a prévia sem disparar o pipeline de análise.
     * O DAT não fornece um Surface de preview; usar capturePhoto aqui garante que a
     * tela representa o mesmo feed enviado pelo dispositivo/Mock Device Kit.
     */
    suspend fun capturePreview(): Boolean {
        val currentStream = stream ?: return false
        if (_streamState.value != StreamState.STREAMING) return false

        return try {
            when (val photo = currentStream.capturePhoto().getOrNull()) {
                is PhotoData.Bitmap -> {
                    _previewPhoto.tryEmit(correctMdkRotation(photo.bitmap))
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao atualizar prévia: ${e.message}")
            false
        }
    }

    /** Converte o frame YUV420 planar (I420) documentado pelo DAT em Bitmap. */
    private fun frameToBitmap(frame: VideoFrame): Bitmap? = try {
        val width = frame.width
        val height = frame.height
        val ySize = width * height
        val uvSize = ySize / 4
        val expectedSize = ySize + (uvSize * 2)
        val source = frame.buffer.duplicate().apply { rewind() }
        if (width <= 0 || height <= 0 || source.remaining() < expectedSize) {
            null
        } else {
            val i420 = ByteArray(expectedSize)
            source.get(i420)
            val nv21 = ByteArray(expectedSize)
            System.arraycopy(i420, 0, nv21, 0, ySize)
            // DAT fornece I420: Y + U + V. YuvImage espera NV21: Y + VU.
            for (index in 0 until uvSize) {
                nv21[ySize + (index * 2)] = i420[ySize + uvSize + index]
                nv21[ySize + (index * 2) + 1] = i420[ySize + index]
            }

            ByteArrayOutputStream().use { output ->
                YuvImage(nv21, ImageFormat.NV21, width, height, null)
                    .compressToJpeg(Rect(0, 0, width, height), 75, output)
                val bytes = output.toByteArray()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Não foi possível converter frame da prévia: ${e.message}")
        null
    }

    // ── Correção de Rotação MDK ───────────────────────────────────────────────

    /**
     * Corrige a rotação de 90° que o MDK aplica ao usar a câmera do celular.
     *
     * Arquitetura §5, item 7:
     * "Quando o MDK está ativo usando a câmera do celular, a imagem retornada
     *  vem rotacionada em 90 graus (comportamento nativo documentado)."
     */
    private fun correctMdkRotation(bitmap: Bitmap): Bitmap {
        if (!BuildConfig.DEBUG) return bitmap  // Sem MDK → sem rotação extra
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    // ── Limpeza ───────────────────────────────────────────────────────────────

    fun stop() {
        stream?.stop()
        stream = null
        _streamState.value = StreamState.STOPPED
        Log.d(TAG, "CameraClient parado.")
    }

    companion object {
        private const val TAG = "DatCameraClient"
    }
}
