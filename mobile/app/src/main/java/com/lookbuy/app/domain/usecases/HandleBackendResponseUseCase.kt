package com.lookbuy.app.domain.usecases

import com.lookbuy.app.domain.models.BackendPayload
import com.lookbuy.app.domain.models.ProductSearchResult

class HandleBackendResponseUseCase {
    fun success(payload: BackendPayload.Success): ProductSearchResult {
        val best = payload.prices.minByOrNull { it.priceCents }
        val text = if (best == null) {
            "Identifiquei ${payload.productName}, mas não encontrei um preço."
        } else {
            "Encontrei ${payload.productName}. O menor preço é ${best.formattedPrice} na ${best.store}."
        }
        return ProductSearchResult(payload.prices, text)
    }
}
