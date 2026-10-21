package com.rippipupil.clothify.data.sources

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThrottleTest {
    @Test
    fun waitsTheMinimumIntervalBetweenCalls() = runTest {
        val throttle = Throttle(3_000) { currentTime }
        throttle.await()
        assertEquals(0L, currentTime)
        throttle.await()
        assertEquals(3_000L, currentTime)
    }
}
