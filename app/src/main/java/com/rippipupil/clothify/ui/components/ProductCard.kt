package com.rippipupil.clothify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.ui.formatPrice
import kotlin.math.roundToInt

/** Línea "Vinted · talla M · a 3 km". */
fun Product.details(): String = listOfNotNull(
    sellerName,
    size?.let { "talla $it" },
    distanceKm?.let { "a ${it.roundToInt().coerceAtLeast(1)} km" },
).joinToString(" · ")

/** Tarjeta grande de Descubrir. Los botones ✕/♥ son alternativas al swipe. */
@Composable
fun ProductCard(
    product: Product,
    onOpen: () -> Unit,
    onDislike: () -> Unit,
    onLike: () -> Unit,
    onAddToBasket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier) {
        Box(Modifier.fillMaxSize().clickable(onClick = onOpen)) {
            ProductImage(product.imageUrl, Modifier.fillMaxSize())
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(formatPrice(product.priceEur), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(product.title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(product.details(), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                TagChips(product.tagIds, max = 4)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = onDislike, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Menos de esto")
                    }
                    FilledTonalIconButton(onClick = onAddToBasket, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = "Añadir a la cesta")
                    }
                    FilledIconButton(
                        onClick = onLike,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Me gusta")
                    }
                }
            }
        }
    }
}
