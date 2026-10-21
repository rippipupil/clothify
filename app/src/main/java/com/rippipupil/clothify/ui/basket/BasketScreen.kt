package com.rippipupil.clothify.ui.basket

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.StoreLinkBuilder
import com.rippipupil.clothify.data.pool.BasketItem
import com.rippipupil.clothify.ui.components.ProductImage
import com.rippipupil.clothify.ui.components.rememberPriceAlertsSetter
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.ui.components.TagChips
import com.rippipupil.clothify.ui.components.details
import com.rippipupil.clothify.ui.containerViewModel
import com.rippipupil.clothify.ui.formatPrice
import com.rippipupil.clothify.ui.openUrl

@Composable
fun BasketScreen(container: AppContainer) {
    val vm = containerViewModel { BasketViewModel(container.basket) }
    val items by vm.items.collectAsState()
    val settings by container.settings.settings.collectAsState(initial = null)
    val location = settings?.lastLocation?.takeIf { settings?.locationEnabled == true }
    val setAlerts = rememberPriceAlertsSetter(container)
    val canCheck: (Source) -> Boolean = container.priceChecker::canCheck
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Tu cesta está vacía. Pulsa 🛒 en una tarjeta para guardarla aquí.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Cesta", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp)) }
        if (settings?.priceAlertsEnabled == false && items.any { canCheck(it.product.source) }) {
            item {
                Card {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("¿Te aviso si baja de precio o se vende algo de tu cesta?", Modifier.weight(1f))
                        Button(onClick = { setAlerts(true) }) { Text("Avisarme") }
                    }
                }
            }
        }
        items(items, key = { it.product.id }) { item -> BasketRow(item, location, canCheck, onRemove = { vm.remove(item.product.id) }) }
    }
}

@Composable
private fun BasketRow(item: BasketItem, location: LatLng?, canCheck: (Source) -> Boolean, onRemove: () -> Unit) {
    val context = LocalContext.current
    val open = { url: String ->
        if (!openUrl(context, url)) Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
    }
    val product = item.product
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProductImage(product.imageUrl, Modifier.size(96.dp))
                Column(Modifier.weight(1f)) {
                    Text(formatPrice(BasketViewModel.displayedPrice(item)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    BasketViewModel.priceLabel(item, canCheck)?.let { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.unavailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(product.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(product.details(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (BasketViewModel.isStale(item) && !canCheck(product.source)) {
                        Text("Puede que ya no esté disponible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Quitar de la cesta") }
            }
            TagChips(product.tagIds, max = 5)
            Button(onClick = { open(product.productUrl) }, modifier = Modifier.fillMaxWidth()) { Text("Comprar en ${product.sellerName}") }
            Text("Buscar parecidos en", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StoreLinkBuilder.links(product.tagIds, location).forEach { (store, url) ->
                    AssistChip(onClick = { open(url) }, label = { Text(store.label) })
                }
            }
        }
    }
}
