package com.rippipupil.clothify

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildConfigTest {
    @Test
    fun geminiModelIsConfigured() {
        assertEquals("gemini-flash-latest", BuildConfig.GEMINI_MODEL)
    }
}
