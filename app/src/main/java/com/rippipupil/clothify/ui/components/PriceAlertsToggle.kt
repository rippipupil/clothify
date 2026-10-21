package com.rippipupil.clothify.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.data.pool.PriceAlerts
import com.rippipupil.clothify.ui.PriceNotifications
import kotlinx.coroutines.launch

/** Devuelve una función que activa o desactiva los avisos de precio (pidiendo permiso si hace falta). */
@Composable
fun rememberPriceAlertsSetter(container: AppContainer): (Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun apply(enabled: Boolean) {
        scope.launch { container.settings.update { it.copy(priceAlertsEnabled = enabled) } }
        if (enabled) PriceAlerts.schedule(context) else PriceAlerts.cancel(context)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) apply(true) else Toast.makeText(context, "Sin permiso de notificaciones no puedo avisarte", Toast.LENGTH_LONG).show()
    }
    return { enabled ->
        if (enabled && !PriceNotifications.hasPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            apply(enabled)
        }
    }
}
