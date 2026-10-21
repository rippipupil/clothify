package com.rippipupil.clothify.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.StoreLinkBuilder
import com.rippipupil.clothify.ui.openUrl

/** "Buscar parecidos en": un botón por tienda. */
@Composable
fun StoreLinksDialog(tagIds: Set<String>, location: LatLng?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar \"${StoreLinkBuilder.queryText(tagIds)}\" en") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreLinkBuilder.links(tagIds, location).forEach { (store, url) ->
                    OutlinedButton(onClick = {
                        if (!openUrl(context, url)) Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text(store.label) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
