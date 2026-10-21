package com.rippipupil.clothify.data.pool

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rippipupil.clothify.ClothifyApp
import com.rippipupil.clothify.ui.PriceNotifications
import java.util.concurrent.TimeUnit

/** Cada 12 h revisa los precios de la cesta y avisa de bajadas y prendas retiradas. */
class PriceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ClothifyApp).container
        if (!container.settings.current().priceAlertsEnabled) return Result.success()
        val alerts = container.priceChecker.check()
        PriceNotifications.show(applicationContext, alerts)
        return Result.success()
    }
}

object PriceAlerts {
    private const val WORK_NAME = "price-check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
