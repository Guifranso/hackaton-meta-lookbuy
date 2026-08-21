package com.lookbuy.app.domain.repository

import com.lookbuy.app.domain.models.ProductPrice

interface PriceRepository {
    suspend fun findPrices(productName: String): List<ProductPrice>
}
