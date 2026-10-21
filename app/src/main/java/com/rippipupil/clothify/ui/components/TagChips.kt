package com.rippipupil.clothify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.domain.TagVocabulary

/** Etiquetas de una prenda, en el orden del vocabulario. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChips(tagIds: Set<String>, modifier: Modifier = Modifier, max: Int = 6) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TagVocabulary.all.filter { it.id in tagIds }.take(max).forEach { tag ->
            AssistChip(onClick = {}, label = { Text(tag.label) })
        }
    }
}
