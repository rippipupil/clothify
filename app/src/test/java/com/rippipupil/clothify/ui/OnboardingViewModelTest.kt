package com.rippipupil.clothify.ui

import androidx.test.core.app.ApplicationProvider
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.style.FailureReason
import com.rippipupil.clothify.data.style.StyleResult
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: ClothifyDatabase
    private lateinit var profile: ProfileRepository
    private lateinit var settings: SettingsRepository
    private var refills = 0

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
        profile = ProfileRepository(db)
        settings = testSettings(tmp)
    }

    @After
    fun tearDown() = db.close()

    private fun vm(analyzer: FakeAnalyzer = FakeAnalyzer(StyleResult.Success(listOf("negro")))) =
        OnboardingViewModel(profile, settings, analyzer, startRefill = { refills++ }, currentLocation = { LatLng(40.42, -3.7) })

    @Test
    fun cannotFinishWithoutAStyle() {
        assertFalse(vm().state.value.canFinish)
    }

    @Test
    fun finishingSavesPresetScoresAndSettings() = runTest {
        val vm = vm()
        vm.togglePreset("streetwear")
        vm.setTopSize("L")
        vm.setMaxPrice(80.0)
        assertTrue(vm.state.value.canFinish)
        vm.finish().join()
        assertEquals(3.0, profile.snapshot().getValue("streetwear"), 1e-9)
        val s = settings.current()
        assertTrue(s.onboardingDone)
        assertEquals("L", s.sizes.top)
        assertEquals(80.0, s.maxPriceEur, 1e-9)
        assertEquals(1, refills)
    }

    @Test
    fun photosAddOwnPhotoScores() = runTest {
        val vm = vm()
        vm.analyzePhotos(listOf(bitmap(), bitmap())).join()
        assertEquals(2, vm.state.value.analyzedPhotos)
        vm.togglePreset("minimalista")
        vm.finish().join()
        assertEquals(3.0 + 2.0 + 2.0, profile.snapshot().getValue("negro"), 1e-9)
    }

    @Test
    fun failedPhotoShowsMessage() = runTest {
        val vm = vm(FakeAnalyzer(StyleResult.Failure(FailureReason.NO_INTERNET)))
        vm.analyzePhotos(listOf(bitmap())).join()
        assertEquals(0, vm.state.value.analyzedPhotos)
        assertNotNull(vm.state.value.message)
    }

    @Test
    fun grantingLocationStoresIt() = runTest {
        val vm = vm()
        vm.onLocationPermission(granted = true).join()
        vm.togglePreset("casual")
        vm.finish().join()
        assertTrue(settings.current().locationEnabled)
        assertEquals(LatLng(40.42, -3.7), settings.current().lastLocation)
    }

    @Test
    fun sizeChipsToggle() {
        val vm = vm()
        vm.setTopSize("M")
        vm.setTopSize("M")
        assertEquals(null, vm.state.value.sizes.top)
    }
}
