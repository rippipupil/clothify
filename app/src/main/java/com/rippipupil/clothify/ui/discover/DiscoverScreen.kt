package com.rippipupil.clothify.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.ui.components.ProductCard
import com.rippipupil.clothify.ui.components.SwipeableCard
import com.rippipupil.clothify.ui.containerViewModel
import com.rippipupil.clothify.ui.openUrl

@Composable
fun DiscoverScreen(container: AppContainer) {
    val vm = containerViewModel { DiscoverViewModel(container.pool, container.profile, container.basket, container.recommender) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    DiscoverContent(
        state = state,
        onSwipe = { product, liked -> vm.onSwipe(product, liked) },
        onAddToBasket = {
            vm.onAddToBasket(it)
            Toast.makeText(context, "Añadida a la cesta", Toast.LENGTH_SHORT).show()
        },
        onOpen = { if (!openUrl(context, it.productUrl)) Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show() },
        onRetry = { vm.onRetry() },
    )
}

@Composable
fun DiscoverContent(
    state: DiscoverUiState,
    onSwipe: (Product, Boolean) -> Unit,
    onAddToBasket: (Product) -> Unit,
    onOpen: (Product) -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        when (state) {
            DiscoverUiState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Buscando prendas…")
            }
            is DiscoverUiState.Empty -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No hay más prendas por ahora", style = MaterialTheme.typography.titleMedium)
                state.warning?.let { Text(it, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onRetry) { Text("Buscar más") }
            }
            is DiscoverUiState.Cards -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.warning?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Box(Modifier.weight(1f)) {
                    // Se dibuja de abajo arriba: la primera de la pila queda encima.
                    state.stack.asReversed().forEachIndexed { index, product ->
                        val isTop = index == state.stack.lastIndex
                        key(product.id) {
                            SwipeableCard(key = product.id, onSwiped = { liked -> onSwipe(product, liked) }, modifier = Modifier.fillMaxSize()) {
                                ProductCard(
                                    product = product,
                                    onOpen = { onOpen(product) },
                                    onDislike = { onSwipe(product, false) },
                                    onLike = { onSwipe(product, true) },
                                    onAddToBasket = { onAddToBasket(product) },
                                    modifier = Modifier.fillMaxSize().then(if (isTop) Modifier else Modifier.padding(top = 8.dp)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
