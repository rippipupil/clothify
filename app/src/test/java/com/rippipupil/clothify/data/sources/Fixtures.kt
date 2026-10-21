package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.UserSizes

fun fixture(name: String): String =
    requireNotNull(Fixtures::class.java.getResource("/fixtures/$name")) { name }.readText()

private object Fixtures

fun query(
    text: String = "sudadera oversize",
    tags: Set<String> = setOf("sudadera", "oversize"),
    category: String? = "sudadera",
    maxPrice: Double = 60.0,
    sizes: UserSizes = UserSizes(),
    location: LatLng? = null,
) = SearchQuery(text, tags, category, maxPrice, sizes, location)
