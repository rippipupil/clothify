package com.rippipupil.clothify.ui.profile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.domain.ProfileScorer
import com.rippipupil.clothify.domain.SearchBudget
import com.rippipupil.clothify.ui.components.SizesAndPrice
import com.rippipupil.clothify.ui.components.loadBitmap
import com.rippipupil.clothify.ui.containerViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun ProfileScreen(container: AppContainer) {
    val vm = containerViewModel {
        ProfileViewModel(
            profile = container.profile,
            settings = container.settings,
            basket = container.basket,
            pool = container.pool,
            analyzer = container.styleAnalyzer,
            currentLocation = { container.location.lastKnown() },
        )
    }
    val scores by vm.scores.collectAsState()
    val settings by vm.userSettings.collectAsState()
    val message by vm.message.collectAsState()
    val analyzing by vm.analyzing.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setLocationEnabled(granted)
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        scope.launch { vm.addPhotos(uris.mapNotNull { loadBitmap(context, it) }) }
    }
    val s = settings ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tu perfil", style = MaterialTheme.typography.headlineSmall)
        message?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = vm::dismissMessage) { Text("OK") }
            }
        }

        Text("Tus estilos", style = MaterialTheme.typography.titleMedium)
        if (scores.isEmpty()) Text("Haz swipe en Descubrir y aquí verás lo que la app va aprendiendo.")
        scores.forEach { (tag, score) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tag.label, Modifier.width(120.dp))
                LinearProgressIndicator(
                    progress = { (abs(score) / ProfileScorer.MAX_SCORE).toFloat() },
                    modifier = Modifier.weight(1f),
                    color = if (score > 0) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                )
                Text(String.format(java.util.Locale("es", "ES"), "%+.1f", score), Modifier.width(48.dp))
            }
        }
        OutlinedButton(
            onClick = { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !analyzing,
        ) { Text(if (analyzing) "Analizando…" else "Añadir fotos de mi estilo") }

        HorizontalDivider()
        SizesAndPrice(s.sizes, s.maxPriceEur, { vm.setTopSize(it) }, { vm.setWaistSize(it) }, { vm.setShoeSize(it) }, { vm.setMaxPrice(it) })

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Segunda mano cerca de mí", style = MaterialTheme.typography.titleSmall)
                Text("Ubicación aproximada para Wallapop", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = s.locationEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !container.location.hasPermission()) {
                        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    } else {
                        vm.setLocationEnabled(enabled)
                    }
                },
            )
        }

        val (today, month) = SearchBudget.remaining(s.budget, LocalDate.now())
        Text("Búsquedas de Google Shopping: quedan $today hoy · $month este mes", style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()
        OutlinedButton(onClick = { vm.resetSeen() }, modifier = Modifier.fillMaxWidth()) { Text("Volver a ver prendas ya vistas") }
        TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Borrar mis datos", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar todos tus datos?") },
            text = { Text("Se borrarán tus gustos, la cesta y los ajustes, y volverás a la bienvenida.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearAll()
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}
