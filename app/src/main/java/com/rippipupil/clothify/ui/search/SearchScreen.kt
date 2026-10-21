package com.rippipupil.clothify.ui.search

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.ui.components.ProductTile
import com.rippipupil.clothify.ui.components.StoreLinksDialog
import com.rippipupil.clothify.ui.components.TagChips
import com.rippipupil.clothify.ui.components.TagPicker
import com.rippipupil.clothify.ui.components.loadBitmap
import com.rippipupil.clothify.ui.containerViewModel
import com.rippipupil.clothify.ui.openUrl
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(container: AppContainer) {
    val vm = containerViewModel { SearchViewModel(container.styleAnalyzer, container.pool, container.settings) }
    val state by vm.state.collectAsState()
    val left by vm.shoppingSearchesLeft.collectAsState()
    val settings by container.settings.settings.collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var linksFor by remember { mutableStateOf<Set<String>?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let(vm::onPhoto)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { loadBitmap(context, uri)?.let(vm::onPhoto) }
    }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        when (val s = state) {
            SearchUiState.Idle -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Busca con una foto", style = MaterialTheme.typography.headlineSmall)
                Text("Saca o elige una foto de una prenda y te enseño prendas reales parecidas.")
                Button(onClick = { camera.launch(null) }) { Text("Hacer foto") }
                OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("Elegir de la galería")
                }
                TextButton(onClick = vm::chooseManually) { Text("Elegir etiquetas a mano") }
                Text(
                    "Las fotos se envían a Google Gemini para analizarlas.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SearchUiState.Analyzing -> Loading("Analizando la foto…")
            is SearchUiState.Searching -> Loading("Buscando en Wallapop, Vinted y Google Shopping…")
            is SearchUiState.Tags -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ajusta las etiquetas", style = MaterialTheme.typography.titleLarge)
                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (s.error != null && s.canRetry) OutlinedButton(onClick = { vm.retry() }) { Text("Reintentar") }
                TagPicker(s.tagIds, vm::toggleTag, Modifier.weight(1f).verticalScroll(rememberScrollState()))
                Text(
                    if (left > 0) "Usa 1 búsqueda de Google Shopping (quedan $left hoy)" else "Sin búsquedas de Google Shopping hoy: solo segunda mano",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = vm::reset) { Text("Otra foto") }
                    OutlinedButton(onClick = { linksFor = s.tagIds }, enabled = s.tagIds.isNotEmpty()) { Text("En las tiendas") }
                    Button(onClick = { vm.search() }, enabled = s.tagIds.isNotEmpty()) { Text("Buscar") }
                }
            }
            is SearchUiState.Results -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChips(s.tagIds, max = 8)
                        s.warning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        if (s.products.isEmpty()) Text("No he encontrado prendas parecidas. Prueba a quitar alguna etiqueta.")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = vm::backToTags) { Text("Editar etiquetas") }
                            OutlinedButton(onClick = { linksFor = s.tagIds }) { Text("Buscar esto en las tiendas") }
                        }
                    }
                }
                items(s.products, key = { it.id }) { product ->
                    ProductTile(
                        product = product,
                        onOpen = { if (!openUrl(context, product.productUrl)) Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show() },
                        onAddToBasket = {
                            scope.launch { container.basket.add(product) }
                            Toast.makeText(context, "Añadida a la cesta", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
    linksFor?.let { tags ->
        StoreLinksDialog(tags, settings?.lastLocation?.takeIf { settings?.locationEnabled == true }) { linksFor = null }
    }
}

@Composable
private fun Loading(text: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text(text, Modifier.padding(top = 12.dp))
    }
}
