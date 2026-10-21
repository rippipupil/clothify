package com.rippipupil.clothify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.domain.TagGroup
import com.rippipupil.clothify.domain.TagVocabulary

private val groupTitles = mapOf(
    TagGroup.CATEGORY to "Prenda",
    TagGroup.STYLE to "Estilo",
    TagGroup.COLOR to "Color",
    TagGroup.FIT_MATERIAL to "Corte y material",
)

/** Todas las etiquetas agrupadas, para quitar o añadir a mano. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPicker(selected: Set<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TagGroup.entries.forEach { group ->
            Text(groupTitles.getValue(group), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagVocabulary.all.filter { it.group == group }.forEach { tag ->
                    FilterChip(selected = tag.id in selected, onClick = { onToggle(tag.id) }, label = { Text(tag.label) })
                }
            }
        }
    }
}
