package com.lookbuy.app.presentation.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lookbuy.app.ai_engine.vision.ProductAnalysisResult
import com.lookbuy.app.R
import com.lookbuy.app.meta_wearables.audio.HfpState
import com.lookbuy.app.presentation.viewmodels.AssistantState
import com.lookbuy.app.ui.theme.LookBuyTheme
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.RegistrationState

// ── Paleta LookBuy ────────────────────────────────────────────────────────────
private val CanvasBg    = Color(0xFFF8F9FE)
private val SurfaceBg   = Color(0xFFFFFFFF)
private val Ink         = Color(0xFF17213D)
private val MutedInk    = Color(0xFF6A748B)
private val AccentBlue  = Color(0xFF2E5CC4)
private val SoftBlue    = Color(0xFFE8EEFC)
private val GreenOk     = Color(0xFF17965A)
private val OrangeWarn  = Color(0xFFDE8B22)
private val RedErr      = Color(0xFFD9505D)
private val MicActive   = Color(0xFF7358D8)
private val MicIdle     = AccentBlue

@Composable
fun MainScreen(
    registrationState: RegistrationState,
    sessionState: DeviceSessionState,
    streamState: StreamState,
    hfpState: HfpState,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    partialText: String,
    lastRecognizedText: String,
    lastCapturedPhoto: Bitmap?,
    cameraPreview: Bitmap?,
    lastAnalysisResult: ProductAnalysisResult?,
    assistantState: AssistantState,
    sessionError: String? = null,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onActivateAssistant: () -> Unit,
    onDeactivateAssistant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CanvasBg, Color(0xFFF0F4FE)))),
    ) {
        HeaderSection(modifier = Modifier.align(Alignment.TopCenter))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Conteúdo central (Scrollable para caber resultado) ────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                cameraPreview?.let { preview ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 1.4f),
                    ) {
                        Box {
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = "Prévia da câmera",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = "PRÉVIA DA CÂMERA",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                            )
                        }
                    }
                }

                // Estados técnicos ficam discretos no vídeo; só aparecem quando há algo a corrigir.
                AnimatedVisibility(visible = sessionError != null) {
                    StatusCard("Conexão", "Erro: ${sessionError?.take(60)}", RedErr)
                }


                // Foto Capturada
                AnimatedVisibility(
                    visible = lastCapturedPhoto != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut()
                ) {
                    lastCapturedPhoto?.let { bmp ->
                        Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Foto Capturada",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Feedback visual de processamento IA
                AnimatedVisibility(visible = isProcessing) {
                    ProcessingIndicator()
                }

                // Resultado da Análise (Fase 4)
                AnimatedVisibility(
                    visible = lastAnalysisResult != null,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut()
                ) {
                    lastAnalysisResult?.let { result ->
                        ResultCard(result)
                    }
                }

                // Área de reconhecimento de fala
                AnimatedVisibility(
                    visible = isListening || partialText.isNotEmpty() || lastRecognizedText.isNotEmpty(),
                    enter = fadeIn(tween(300)),
                    exit  = fadeOut(tween(300)),
                ) {
                    SpeechBubble(
                        isListening = isListening,
                        partialText = partialText,
                        lastText    = lastRecognizedText,
                        isSpeaking  = isSpeaking,
                    )
                }
            }

            // ── Botão de microfone + footer ───────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // O mic fica habilitado assim que o registro Meta AI for confirmado.
                // A câmera só é necessária para enviar a foto; a voz sempre funciona.
                val micEnabled = registrationState == RegistrationState.REGISTERED
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        enabled = micEnabled,
                        onClick = {
                            if (assistantState == AssistantState.INACTIVE) onActivateAssistant()
                            else onDeactivateAssistant()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFD9DEEA),
                            disabledContentColor = MutedInk,
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(3f)
                            .height(56.dp),
                    ) {
                        Text(
                            if (assistantState == AssistantState.INACTIVE) "Ativar assistente"
                            else "Encerrar assistente",
                            fontSize = 13.sp,
                        )
                    }
                    MicButton(
                        isListening  = isListening,
                        isSpeaking   = isSpeaking,
                        enabled      = micEnabled && assistantState != AssistantState.INACTIVE,
                        onPress      = onMicPress,
                        onRelease    = onMicRelease,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(AccentBlue)
            .padding(vertical = 12.dp, horizontal = 28.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.lookbuy_logo),
            contentDescription = "LookBuy",
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .height(42.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun StatusCard(label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = MutedInk, fontSize = 11.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                AnimatedContent(
                    targetState = value,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                    label = "status",
                ) { Text(it, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun ReadyBanner() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = AccentBlue)
            Column {
                Text("Pronto para encontrar preços", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Ative o assistente e segure o mic para perguntar.", color = MutedInk, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProcessingIndicator() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = AccentBlue,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.width(12.dp))
            Text("Analisando o produto e pesquisando preços...", color = Ink, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResultCard(result: ProductAnalysisResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(result.productName, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Marca: ${result.brand}", color = MutedInk, fontSize = 11.sp)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftBlue)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Preço estimado", color = AccentBlue, fontWeight = FontWeight.Medium)
                    }
                    Text(result.formattedPrice, color = AccentBlue, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SpeechBubble(
    isListening: Boolean,
    partialText: String,
    lastText: String,
    isSpeaking: Boolean,
) {
    val displayText = when {
        isListening && partialText.isNotEmpty() -> partialText
        isListening                             -> "Ouvindo..."
        isSpeaking                              -> lastText
        else                                    -> lastText
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Filled.Mic,
                contentDescription = null,
                tint = AccentBlue,
            )
            Text(
                text = displayText,
                color = Ink.copy(alpha = if (isListening && partialText.isEmpty()) 0.55f else 0.9f),
                fontSize = 12.sp,
                fontStyle = if (isListening && partialText.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    enabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Ao pressionar, o botão cresce levemente para dar retorno visual imediato.
    val pressScale by animateFloatAsState(
        targetValue = when {
            isListening && isPressed -> 1.035f
            isListening              -> 1.02f
            isPressed                -> 1.03f
            else                     -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "press_scale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .scale(pressScale)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    when {
                        !enabled -> Color(0xFFD9DEEA)
                        isPressed || isListening -> Color(0xFF6389E5)
                        else -> AccentBlue
                    }
                )
                .pointerInput(enabled) {
                    detectTapGestures(
                        onPress = { _ ->
                            if (enabled) {
                                onPress()
                                tryAwaitRelease()
                                onRelease()
                            }
                        }
                    )
                },
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isListening) "Parar escuta" else "Iniciar escuta",
                tint = if (enabled) Color.White else MutedInk,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}


// ── Extensões ─────────────────────────────────────────────────────────────────

private fun RegistrationState.toLabel() = when (this) {
    RegistrationState.UNAVAILABLE   -> "SDK indisponível"
    RegistrationState.AVAILABLE     -> "Disponível"
    RegistrationState.REGISTERING   -> "Registrando..."
    RegistrationState.UNREGISTERING -> "Cancelando..."
    RegistrationState.REGISTERED    -> "Registrado ✓"
}
private fun RegistrationState.toColor() = when (this) {
    RegistrationState.REGISTERED              -> GreenOk
    RegistrationState.REGISTERING,
    RegistrationState.AVAILABLE               -> OrangeWarn
    else                                       -> RedErr
}

private fun DeviceSessionState.toLabel() = when (this) {
    DeviceSessionState.IDLE     -> "Aguardando"
    DeviceSessionState.STARTING -> "Iniciando..."
    DeviceSessionState.STARTED  -> "Conectado ✓"
    DeviceSessionState.PAUSED   -> "Pausado"
    DeviceSessionState.STOPPING -> "Parando..."
    DeviceSessionState.STOPPED  -> "Parado"
}
private fun DeviceSessionState.toColor() = when (this) {
    DeviceSessionState.STARTED            -> GreenOk
    DeviceSessionState.STARTING,
    DeviceSessionState.STOPPING,
    DeviceSessionState.PAUSED             -> OrangeWarn
    else                                   -> RedErr
}

private fun StreamState.toLabel() = when (this) {
    StreamState.STARTING  -> "Iniciando stream..."
    StreamState.STARTED   -> "Stream iniciado"
    StreamState.STREAMING -> "Ao vivo ✓"
    StreamState.STOPPING  -> "Parando..."
    StreamState.STOPPED   -> "Parado"
    StreamState.PAUSED    -> "Pausado"
    StreamState.CLOSED    -> "Fechado"
}
private fun StreamState.toColor() = when (this) {
    StreamState.STREAMING -> GreenOk
    StreamState.STARTING,
    StreamState.STARTED,
    StreamState.PAUSED,
    StreamState.STOPPING  -> OrangeWarn
    else                  -> RedErr
}


private fun HfpState.toLabel() = when (this) {
    HfpState.DISCONNECTED -> "Desconectado"
    HfpState.CONNECTING   -> "Conectando..."
    HfpState.ACTIVE       -> "HFP ativo ✓"
}
private fun HfpState.toColor() = when (this) {
    HfpState.ACTIVE       -> GreenOk
    HfpState.CONNECTING   -> OrangeWarn
    HfpState.DISCONNECTED -> RedErr
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun MainScreenReadyPreview() {
    LookBuyTheme {
        MainScreen(
            registrationState = RegistrationState.REGISTERED,
            sessionState      = DeviceSessionState.STARTED,
            streamState       = StreamState.STREAMING,
            hfpState          = HfpState.ACTIVE,
            isListening       = false,
            isSpeaking        = false,
            isProcessing      = false,
            partialText       = "",
            lastRecognizedText = "",
            lastCapturedPhoto = null,
            cameraPreview = null,
            lastAnalysisResult = null,
            assistantState = AssistantState.INACTIVE,
            onMicPress        = {},
            onMicRelease      = {},
            onActivateAssistant = {},
            onDeactivateAssistant = {},
        )
    }
}
