package com.rippipupil.clothify.data.location

import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.domain.LatLng

/** Al abrir la app: si la ubicación está activada, guarda la última conocida (si hay). */
suspend fun refreshLocation(settings: SettingsRepository, current: suspend () -> LatLng?) {
    if (!settings.current().locationEnabled) return
    val location = current() ?: return
    settings.update { it.copy(lastLocation = location) }
}
