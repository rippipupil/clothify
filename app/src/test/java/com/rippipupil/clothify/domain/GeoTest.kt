package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {
    @Test
    fun madridToBarcelona() {
        val d = Geo.distanceKm(LatLng(40.4168, -3.7038), LatLng(41.3874, 2.1686))
        assertEquals(505.0, d, 5.0)
    }
}
