package com.lookbuy.app

import android.app.Application
import android.util.Log
import com.meta.wearable.dat.core.Wearables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ponto de entrada da aplicação.
 *
 * Regra crítica do SDK (Arquitetura §5, item 2):
 * [Wearables.initialize] deve ser chamado UMA ÚNICA VEZ aqui no [onCreate].
 * Chamar em outro lugar lançará NOT_INITIALIZED.
 *
 * Nota: initialize é uma suspend fun — executada em uma coroutine dedicada.
 */
class LookBuyApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                Wearables.initialize(this@LookBuyApplication)
                Log.d(TAG, "Wearables SDK inicializado com sucesso.")
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao inicializar Wearables SDK: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "LookBuyApplication"
    }
}
