package com.rippipupil.clothify.data

import com.rippipupil.clothify.data.location.refreshLocation
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.ui.testSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocationRefreshTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun updatesLocationWhenEnabled() = runTest {
        val settings = testSettings(tmp)
        settings.update { it.copy(locationEnabled = true, lastLocation = LatLng(40.0, -3.0)) }
        refreshLocation(settings) { LatLng(41.39, 2.17) }
        assertEquals(LatLng(41.39, 2.17), settings.current().lastLocation)
    }

    @Test
    fun keepsOldLocationWhenNoFix() = runTest {
        val settings = testSettings(tmp)
        settings.update { it.copy(locationEnabled = true, lastLocation = LatLng(40.0, -3.0)) }
        refreshLocation(settings) { null }
        assertEquals(LatLng(40.0, -3.0), settings.current().lastLocation)
    }

    @Test
    fun doesNothingWhenDisabled() = runTest {
        val settings = testSettings(tmp)
        var asked = false
        refreshLocation(settings) { asked = true; LatLng(1.0, 1.0) }
        assertEquals(false, asked)
        assertEquals(null, settings.current().lastLocation)
    }
}
