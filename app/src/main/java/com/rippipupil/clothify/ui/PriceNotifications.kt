package com.rippipupil.clothify.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rippipupil.clothify.R
import com.rippipupil.clothify.data.pool.PriceAlert

object PriceNotifications {
    private const val CHANNEL_ID = "price_alerts"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, alerts: List<PriceAlert>) {
        if (alerts.isEmpty() || !hasPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Bajadas de precio", NotificationManager.IMPORTANCE_DEFAULT),
        )
        alerts.forEach { alert ->
            val (title, text) = when (alert) {
                is PriceAlert.Dropped ->
                    "Ha bajado de precio" to "${alert.product.title}: ${formatPrice(alert.fromEur)} → ${formatPrice(alert.toEur)}"
                is PriceAlert.Gone -> "Ya no está disponible" to alert.product.title
            }
            val open = PendingIntent.getActivity(
                context,
                alert.product.id.hashCode(),
                Intent(Intent.ACTION_VIEW, Uri.parse(alert.product.productUrl)),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            @Suppress("MissingPermission") // comprobado en hasPermission()
            NotificationManagerCompat.from(context).notify(alert.product.id.hashCode(), notification)
        }
    }
}
