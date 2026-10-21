package com.rippipupil.clothify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.ui.formatPrice

/** Tarjeta pequeña para cuadrículas de resultados. */
@Composable
fun ProductTile(product: Product, onOpen: () -> Unit, onAddToBasket: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onOpen)) {
        ProductImage(product.imageUrl, Modifier.fillMaxWidth().aspectRatio(0.8f))
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatPrice(product.priceEur), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onAddToBasket) { Icon(Icons.Filled.AddShoppingCart, contentDescription = "Añadir a la cesta") }
            }
            Text(product.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Text(product.details(), maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
