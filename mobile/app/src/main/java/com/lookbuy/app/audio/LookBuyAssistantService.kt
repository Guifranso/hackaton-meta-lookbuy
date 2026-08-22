package com.lookbuy.app.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.lookbuy.app.R

/**
 * Mantém o processo do LookBuy visível para o Android enquanto o assistente está
 * ativado pelo usuário. A futura wake word/VAD deve rodar sob este serviço.
 *
 * O serviço nunca inicia sozinho: ele é iniciado pela Activity visível após o
 * consentimento explícito do usuário para o uso do microfone.
 */
class LookBuyAssistantService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.assistant_notification_title))
                .setContentText(getString(R.string.assistant_notification_text))
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            },
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.assistant_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "lookbuy_assistant"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.lookbuy.app.action.STOP_ASSISTANT"

        fun start(context: Context) {
            val intent = Intent(context, LookBuyAssistantService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LookBuyAssistantService::class.java).setAction(ACTION_STOP))
        }
    }
}
