package com.lookbuy.app.data.repository

import com.lookbuy.app.domain.models.ProductPrice
import com.lookbuy.app.domain.repository.PriceRepository
import kotlinx.coroutines.delay

/** Local deterministic source; swap it for a Retrofit PriceApiService in production. */
class PriceRepositoryImpl : PriceRepository {
    override suspend fun findPrices(productName: String): List<ProductPrice> {
        delay(650)
        return listOf(
            ProductPrice("Loja Parceira", 8990),
            ProductPrice("Marketplace", 9490),
            ProductPrice("Varejista local", 10290)
        )
    }
}
