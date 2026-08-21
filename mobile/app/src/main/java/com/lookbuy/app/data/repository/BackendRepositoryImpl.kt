package com.lookbuy.app.data.repository

import com.lookbuy.app.data.remote.ApiService
import com.lookbuy.app.domain.models.BackendPayload
import com.lookbuy.app.domain.repository.BackendRepository

class BackendRepositoryImpl(private val api: ApiService) : BackendRepository {
    override suspend fun sendSecureFrame(image: ByteArray, hint: String) = api.analyzeFrame(image, hint)
    override suspend fun sendClarification(answer: String): BackendPayload = api.resolveClarification(answer)
}
