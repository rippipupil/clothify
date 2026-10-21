package com.rippipupil.clothify.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rippipupil.clothify.domain.LatLng
import kotlinx.coroutines.tasks.await
import kotlin.math.round

/** Ubicación aproximada (~1 km), solo si el usuario ha dado permiso. */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): LatLng? {
        if (!hasPermission()) return null
        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client.lastLocation.await()
                ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
            location?.let { LatLng(round2(it.latitude), round2(it.longitude)) }
        }.getOrNull()
    }

    private fun round2(value: Double) = round(value * 100) / 100
}
