package com.lookbuy.app.domain.usecases

import android.graphics.Bitmap
import android.util.Log
import com.lookbuy.app.ai_engine.speech.TtsClient
import com.lookbuy.app.ai_engine.vision.ProductAnalysisResult
import com.lookbuy.app.ai_engine.vision.VisionClient

/**
 * Orquestra a Fase 4: Análise do Produto com VLM.
 * 
 * Passos:
 * 1. Ouve o áudio de "processando" via TTS.
 * 2. Envia o Bitmap (da câmera MDK) e o Texto (do STT) para o VLM.
 * 3. Recebe a resposta e lê no TTS o preço/produto encontrado.
 * 4. Retorna o resultado para ser exibido na UI do smartphone.
 */
class ProcessProductLookUseCase(
    private val visionClient: VisionClient,
    private val ttsClient: TtsClient
) {
    suspend operator fun invoke(image: Bitmap, speechText: String): ProductAnalysisResult? {
        Log.d(TAG, "Iniciando processamento. Texto: '$speechText'")
        
        // 1. Feedback imediato ao usuário (áudio no fone HFP)
        ttsClient.speak("Processando imagem e buscando preços...")

        return try {
            // 2. Chama a API de visão
            val result = visionClient.analyzeProduct(image, speechText)
            
            // 3. Lê o resultado no fone
            Log.d(TAG, "Resultado obtido: ${result.productName} por ${result.formattedPrice}")
            ttsClient.speak(result.ttsFeedback)
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar produto: ${e.message}", e)
            ttsClient.speak("Desculpe, não consegui analisar o produto no momento.")
            null
        }
    }

    companion object {
        private const val TAG = "ProcessProductLook"
    }
}
