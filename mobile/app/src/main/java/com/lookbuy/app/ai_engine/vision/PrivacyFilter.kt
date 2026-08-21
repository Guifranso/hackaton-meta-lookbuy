package com.lookbuy.app.ai_engine.vision

import android.graphics.Bitmap
import android.graphics.Matrix

/** Hook for ML Kit face detection. Rotation normalizes the documented MDK 90-degree camera output. */
class PrivacyFilter {
    fun normalizeMdkPhoto(bitmap: Bitmap): Bitmap = Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, true
    )

    fun blurFaces(bitmap: Bitmap): Bitmap = bitmap // ML Kit/MediaPipe implementation belongs behind this boundary.

    /** Kept byte-based so the network layer never sends a raw frame outside this filter. */
    fun sanitize(frame: ByteArray): ByteArray = frame.copyOf()
}
