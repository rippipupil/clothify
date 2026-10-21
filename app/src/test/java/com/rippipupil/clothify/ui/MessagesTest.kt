package com.rippipupil.clothify.ui

import com.rippipupil.clothify.data.sources.SourceFailure
import com.rippipupil.clothify.domain.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessagesTest {
    @Test
    fun allOfflineIsASingleMessage() = assertEquals(
        "Sin conexión",
        sourceWarning(mapOf(Source.WALLAPOP to SourceFailure.NO_INTERNET, Source.VINTED to SourceFailure.NO_INTERNET)),
    )

    @Test
    fun offlineIsNotRepeated() = assertEquals(
        "Sin conexión · Vinted no responde ahora",
        sourceWarning(
            mapOf(
                Source.WALLAPOP to SourceFailure.NO_INTERNET,
                Source.GOOGLE_SHOPPING to SourceFailure.NO_INTERNET,
                Source.VINTED to SourceFailure.BLOCKED,
            ),
        ),
    )

    @Test
    fun disabledSourcesAreSilent() = assertNull(sourceWarning(mapOf(Source.GOOGLE_SHOPPING to SourceFailure.DISABLED)))
}
