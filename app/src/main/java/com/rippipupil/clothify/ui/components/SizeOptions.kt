package com.rippipupil.clothify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.data.settings.UserSettings
import com.rippipupil.clothify.domain.UserSizes
import com.rippipupil.clothify.ui.formatPrice

val TOP_SIZES = listOf("XS", "S", "M", "L", "XL", "XXL")
val WAIST_SIZES = (28..40 step 2).map { it.toString() }
val SHOE_SIZES = (38..47).map { it.toString() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(title: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
            }
        }
    }
}

/** Tallas (opcionales: tocar de nuevo quita la selección) y precio máximo. */
@Composable
fun SizesAndPrice(
    sizes: UserSizes,
    maxPriceEur: Double,
    onTop: (String) -> Unit,
    onWaist: (String) -> Unit,
    onShoe: (String) -> Unit,
    onMaxPrice: (Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ChipRow("Parte de arriba", TOP_SIZES, sizes.top, onTop)
        ChipRow("Pantalón (cintura)", WAIST_SIZES, sizes.waist, onWaist)
        ChipRow("Calzado (EU)", SHOE_SIZES, sizes.shoe, onShoe)
        Column {
            Text("Precio máximo: ${formatPrice(maxPriceEur)}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = maxPriceEur.toFloat(),
                onValueChange = { onMaxPrice((it / 5).toInt() * 5.0) },
                valueRange = UserSettings.MIN_PRICE.toFloat()..UserSettings.MAX_PRICE.toFloat(),
            )
        }
    }
}
