package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.SearchQuery

/** Descarta solo si sabemos la talla de la prenda y no es la del usuario. */
internal fun SearchQuery.acceptsSize(size: String?): Boolean {
    val wanted = wantedSize ?: return true
    return size == null || size.equals(wanted, ignoreCase = true)
}

internal fun Double.asQueryNumber(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
