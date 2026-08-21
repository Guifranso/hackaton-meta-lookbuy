package com.lookbuy.app.data.remote

import com.lookbuy.app.domain.models.BackendPayload
import com.lookbuy.app.domain.models.ProductPrice
import kotlinx.coroutines.delay

/** HTTP/WS boundary for FastAPI. Replace this deterministic local implementation with Retrofit/Ktor. */
interface ApiService {
    suspend fun analyzeFrame(image: ByteArray, hint: String): BackendPayload
    suspend fun resolveClarification(answer: String): BackendPayload
}

class DemoApiService : ApiService {
    override suspend fun analyzeFrame(image: ByteArray, hint: String): BackendPayload {
        delay(700)
        return when {
            hint.contains("?", ignoreCase = true) || hint.contains("varia", ignoreCase = true) ->
                BackendPayload.Clarification("Qual é a variação, tamanho ou sabor do produto?")
            hint.isBlank() || hint.contains("não", ignoreCase = true) ->
                BackendPayload.Failure("Não consegui identificar o produto com segurança.")
            else -> success(hint)
        }
    }

    override suspend fun resolveClarification(answer: String): BackendPayload {
        delay(450)
        return if (answer.isBlank()) BackendPayload.Failure("Não recebi uma resposta para confirmar o item.") else success(answer)
    }

    private fun success(product: String) = BackendPayload.Success(
        productName = product,
        prices = listOf(
            ProductPrice("Loja Parceira", 8990),
            ProductPrice("Marketplace", 9490),
            ProductPrice("Varejista local", 10290)
        ),
        promotion = "Menor preço encontrado agora"
    )
}
