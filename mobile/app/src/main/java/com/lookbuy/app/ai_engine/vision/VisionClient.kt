package com.lookbuy.app.ai_engine.vision

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

data class ProductAnalysisResult(
    val productName: String,
    val brand: String,
    val estimatedPrice: Double,
    val confidence: Float,
    val isFound: Boolean,
    val ttsFeedback: String
) {
    val formattedPrice: String
        get() = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(estimatedPrice)
}

/**
 * Cliente de Visão Computacional (VLM).
 * Recebe a foto da câmera e o texto extraído do STT para identificar o produto.
 * 
 * Implementação Mock: Retorna dados estáticos para simulação (Fase 4).
 */
class VisionClient {

    suspend fun analyzeProduct(image: Bitmap, speechText: String): ProductAnalysisResult {
        // Simula o delay de rede de uma chamada a API VLM (ex: Gemini/OpenAI)
        delay(2000)

        // Mock baseado no texto falado (para ser dinâmico no teste)
        val isShoe = speechText.contains("tênis", ignoreCase = true) || speechText.contains("nike", ignoreCase = true)
        
        return if (isShoe) {
            ProductAnalysisResult(
                productName = "Tênis Air Max 90",
                brand = "Nike",
                estimatedPrice = 799.90,
                confidence = 0.95f,
                isFound = true,
                ttsFeedback = "Encontrei o Tênis Nike Air Max 90 por aproximadamente 800 reais."
            )
        } else {
            ProductAnalysisResult(
                productName = speechText.takeIf { it.isNotBlank() } ?: "Produto Desconhecido",
                brand = "Genérica",
                estimatedPrice = 149.90,
                confidence = 0.82f,
                isFound = true,
                ttsFeedback = "Encontrei $speechText por aproximadamente 150 reais."
            )
        }
    }
}
