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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lookbuy.app.ai_engine.vision.ProductAnalysisResult
import com.lookbuy.app.meta_wearables.audio.HfpState
import com.lookbuy.app.presentation.viewmodels.AssistantState
import com.lookbuy.app.ui.theme.LookBuyTheme
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.RegistrationState

// ── Paleta LookBuy ────────────────────────────────────────────────────────────
private val DarkBg      = Color(0xFF080C14)
private val CardBg      = Color(0xFF111827)
private val AccentBlue  = Color(0xFF4F8EFF)
private val AccentPurple= Color(0xFF9B59F5)
private val GreenOk     = Color(0xFF27AE60)
private val OrangeWarn  = Color(0xFFE67E22)
private val RedErr      = Color(0xFFE74C3C)
private val MicActive   = Color(0xFFE74C3C)
private val MicIdle     = Color(0xFF4F8EFF)

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
            .background(Brush.verticalGradient(listOf(DarkBg, Color(0xFF0B1122)))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Header ────────────────────────────────────────────────────────
            HeaderSection(sessionState = sessionState)

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
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f),
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

                // Cards de status (esconde ao mostrar resultado pra dar espaço)
                AnimatedVisibility(visible = lastAnalysisResult == null && !isProcessing) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatusCard("Registro Meta AI", registrationState.toLabel(), registrationState.toColor())
                        // Se há erro de sessão mostra em vermelho
                        val sessionLabel = if (sessionError != null)
                            "Erro: ${sessionError.take(60)}"
                        else sessionState.toLabel()
                        StatusCard("Sessão DAT", sessionLabel, if (sessionError != null) RedErr else sessionState.toColor())
                        StatusCard("Câmera DAT",  streamState.toLabel(), streamState.toColor())
                        StatusCard("Áudio HFP",   hfpState.toLabel(),   hfpState.toColor())
                    }
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
                                .aspectRatio(16f / 9f)
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

                // Banner "Sistema pronto"
                AnimatedVisibility(
                    visible = registrationState == RegistrationState.REGISTERED &&
                              sessionState == DeviceSessionState.STARTED &&
                              streamState == StreamState.STREAMING &&
                              lastCapturedPhoto == null &&
                              !isProcessing,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit  = fadeOut() + scaleOut(),
                ) {
                    ReadyBanner()
                }

                // Área de reconhecimento de fala
                AnimatedVisibility(
                    visible = (isListening || partialText.isNotEmpty() || lastRecognizedText.isNotEmpty()) && lastAnalysisResult == null,
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
                Button(
                    enabled = micEnabled,
                    onClick = {
                        if (assistantState == AssistantState.INACTIVE) onActivateAssistant()
                        else onDeactivateAssistant()
                    },
                ) {
                    Text(
                        if (assistantState == AssistantState.INACTIVE) "Ativar assistente"
                        else "Encerrar assistente",
                    )
                }
                MicButton(
                    isListening  = isListening,
                    isSpeaking   = isSpeaking,
                    enabled      = micEnabled && assistantState != AssistantState.INACTIVE,
                    onPress      = onMicPress,
                    onRelease    = onMicRelease,
                )
                Text(
                    text = when {
                        !micEnabled              -> "Aguardando registro Meta AI..."
                        assistantState == AssistantState.LISTENING_FOR_WAKE_WORD ->
                            "Assistente ativo. MVP: segure o microfone para falar."
                        assistantState == AssistantState.INACTIVE ->
                            "Ative o assistente para usar o microfone"
                        isListening              -> "Solte para processar"
                        streamState == StreamState.STREAMING -> if (hfpState == HfpState.ACTIVE)
                            "Segure para falar (Áudio via Bluetooth)"
                        else
                            "Segure para falar (Áudio do Celular)"
                        else                     -> "Segure para falar (câmera conectando...)"
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(sessionState: DeviceSessionState) {
    val isConnected = sessionState == DeviceSessionState.STARTED
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.07f else 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOutCubic), RepeatMode.Reverse
        ),
        label = "pulse",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(AccentBlue.copy(alpha = 0.22f), Color.Transparent)
                    )
                ),
        ) {
            Text("🕶️", fontSize = 46.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "LookBuy",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            "Comparador inteligente mãos-livres",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun StatusCard(label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
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
                ) { Text(it, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun ReadyBanner() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GreenOk.copy(alpha = 0.13f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = GreenOk)
            Column {
                Text("Sistema pronto!", color = GreenOk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Segure o mic e diga o produto.", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProcessingIndicator() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.15f)),
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
                color = AccentPurple,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.width(12.dp))
            Text("A IA está analisando a imagem...", color = AccentPurple, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResultCard(result: ProductAnalysisResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
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
                    Text(result.productName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Marca: ${result.brand}", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenOk.copy(alpha = 0.15f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = GreenOk, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Preço Encontrado", color = GreenOk, fontWeight = FontWeight.Medium)
                    }
                    Text(result.formattedPrice, color = GreenOk, fontSize = 22.sp, fontWeight = FontWeight.Black)
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
        isSpeaking                              -> "🔊 $lastText"
        else                                    -> lastText
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
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
                tint = if (isSpeaking) AccentPurple else AccentBlue,
            )
            Text(
                text = displayText,
                color = Color.White.copy(alpha = if (isListening && partialText.isEmpty()) 0.45f else 0.9f),
                fontSize = 14.sp,
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
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Pulso infinito quando está escutando
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "ring_alpha",
    )

    // Escala: comprime ao pressionar (0.88), bounce ao soltar, normaliza
    val pressScale by animateFloatAsState(
        targetValue = when {
            isListening && isPressed -> 0.88f
            isListening              -> 1.05f
            isPressed                -> 0.88f
            else                     -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "press_scale",
    )

    // Anel externo expande ao pressionar
    val ringScale by animateFloatAsState(
        targetValue = if (isPressed || isListening) 1.35f else 1f,
        animationSpec = tween(200),
        label = "ring_scale",
    )


    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp),
    ) {
        // Anel externo pulsante (visível ao pressionar ou ao escutar)
        if (isListening || isPressed) {
            Box(
                Modifier
                    .size(120.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(
                        if (isListening) MicActive.copy(alpha = ringAlpha * 0.4f)
                        else MicIdle.copy(alpha = 0.25f)
                    )
            )
        }

        // Botão principal
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .scale(pressScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (!enabled) listOf(Color.Gray, Color.DarkGray)
                        else if (isListening) listOf(MicActive, MicActive.copy(alpha = 0.6f))
                        else listOf(MicIdle, AccentPurple)
                    )
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
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp),
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
