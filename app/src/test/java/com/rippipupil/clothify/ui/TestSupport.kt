package com.rippipupil.clothify.ui

import android.graphics.Bitmap
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.style.StyleAnalyzer
import com.rippipupil.clothify.data.style.StyleResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

fun testSettings(tmp: TemporaryFolder) = SettingsRepository(
    PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
        tmp.newFile("settings-${System.nanoTime()}.preferences_pb")
    },
)

class FakeAnalyzer(var result: StyleResult) : StyleAnalyzer {
    var calls = 0
    override suspend fun analyze(image: Bitmap): StyleResult = result.also { calls++ }
}

fun bitmap(): Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
