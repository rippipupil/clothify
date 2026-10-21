package com.rippipupil.clothify.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.domain.StylePresets
import com.rippipupil.clothify.ui.components.SizesAndPrice
import com.rippipupil.clothify.ui.components.loadBitmap
import com.rippipupil.clothify.ui.containerViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(container: AppContainer) {
    val vm = containerViewModel {
        OnboardingViewModel(
            profile = container.profile,
            settings = container.settings,
            analyzer = container.styleAnalyzer,
            startRefill = { container.appScope.launch { container.pool.refill() } },
            currentLocation = { container.location.lastKnown() },
        )
    }
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        Text("Clothify", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.step + 1) / (OnboardingViewModel.LAST_STEP + 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f)) {
            when (state.step) {
                0 -> SizesStep(state, vm)
                1 -> LocationStep(state, vm)
                else -> StyleStep(state, vm)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (state.step > 0) TextButton(onClick = vm::back) { Text("Atrás") } else Spacer(Modifier)
            if (state.step < OnboardingViewModel.LAST_STEP) {
                Button(onClick = vm::next) { Text("Siguiente") }
            } else {
                Button(onClick = { vm.finish() }, enabled = state.canFinish) { Text("Empezar") }
            }
        }
    }
}

@Composable
private fun SizesStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tus tallas y tu precio", style = MaterialTheme.typography.titleLarge)
        Text("Las tallas son opcionales: en Vinted y Wallapop se ocultan las prendas de otra talla.")
        SizesAndPrice(state.sizes, state.maxPriceEur, vm::setTopSize, vm::setWaistSize, vm::setShoeSize, vm::setMaxPrice)
    }
}

@Composable
private fun LocationStep(state: OnboardingState, vm: OnboardingViewModel) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.onLocationPermission(granted)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Segunda mano cerca de ti", style = MaterialTheme.typography.titleLarge)
        Text(
            "Con tu ubicación aproximada (unos 1 km), Wallapop te enseña primero lo que está cerca y " +
                "ves a cuántos km está cada prenda. No sale del móvil salvo para buscar en Wallapop.",
        )
        if (state.locationEnabled) {
            Text("✓ Ubicación activada", color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Text("Activar ubicación") }
            TextButton(onClick = vm::next) { Text("Ahora no") }
        }
    }
}

@Composable
private fun StyleStep(state: OnboardingState, vm: OnboardingViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(OnboardingViewModel.MAX_PHOTOS),
    ) { uris ->
        scope.launch { vm.analyzePhotos(uris.mapNotNull { loadBitmap(context, it) }) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("¿Qué estilos te gustan?", style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(StylePresets.all) { preset ->
                FilterChip(
                    selected = preset.id in state.selectedPresets,
                    onClick = { vm.togglePreset(preset.id) },
                    label = { Text(preset.label) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !state.analyzing,
            ) { Text("Subir fotos de mi ropa") }
            if (state.analyzing) CircularProgressIndicator(Modifier.height(24.dp))
            if (state.analyzedPhotos > 0) Text("${state.analyzedPhotos} analizadas")
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            "Las fotos se envían a Google Gemini para analizarlas. En el plan gratuito Google puede usarlas para mejorar sus productos.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
