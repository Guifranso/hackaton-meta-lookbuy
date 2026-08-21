package com.lookbuy.app.domain.models

enum class ConfidenceLevel { HIGH, AMBIGUOUS, INSUFFICIENT }

/** Payloads returned by the vision/price service described in the architecture. */
sealed interface BackendPayload {
    data class Success(
        val productName: String,
        val prices: List<ProductPrice>,
        val promotion: String? = null
    ) : BackendPayload

    data class Clarification(val question: String) : BackendPayload
    data class Failure(val reason: String) : BackendPayload
}

data class CapturedFrame(val bytes: ByteArray, val rotationDegrees: Int = 0)

data class ProductPrice(
    val store: String,
    val priceCents: Int,
    val url: String = ""
) {
    val formattedPrice: String get() = "R$ %.2f".format(priceCents / 100.0)
}

data class ProductSearchResult(
    val prices: List<ProductPrice> = emptyList(),
    val spokenResponse: String
)
