package com.rippipupil.clothify

import android.app.Application
import com.rippipupil.clothify.data.pool.PriceAlerts
import kotlinx.coroutines.launch

class ClothifyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            if (container.settings.current().priceAlertsEnabled) PriceAlerts.schedule(this@ClothifyApp)
        }
    }
}
