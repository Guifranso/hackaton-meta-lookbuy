package com.lookbuy.app.domain.repository

import com.lookbuy.app.domain.models.BackendPayload

interface BackendRepository {
    suspend fun sendSecureFrame(image: ByteArray, hint: String): BackendPayload
    suspend fun sendClarification(answer: String): BackendPayload
}
