package com.rippipupil.clothify.ui

import androidx.test.core.app.ApplicationProvider
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.pool.BasketItem
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.style.StyleResult
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.QueryPlanner
import com.rippipupil.clothify.domain.product
import com.rippipupil.clothify.ui.basket.BasketViewModel
import com.rippipupil.clothify.ui.profile.ProfileViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BasketAndProfileViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: ClothifyDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var profile: ProfileRepository
    private lateinit var basket: BasketRepository

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
        settings = testSettings(tmp)
        profile = ProfileRepository(db)
        basket = BasketRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun profileVm(analyzer: FakeAnalyzer = FakeAnalyzer(StyleResult.Success(listOf("verde")))) = ProfileViewModel(
        profile = profile,
        settings = settings,
        basket = basket,
        pool = ProductPoolRepository(db, emptyList(), QueryPlanner(Random(1)), profile, settings),
        analyzer = analyzer,
        currentLocation = { null },
    )

    @Test
    fun basketRemovesProducts() = runTest {
        basket.add(product("a"))
        basket.add(product("b"))
        val vm = BasketViewModel(basket)
        vm.remove("a").join()
        assertEquals(listOf("b"), vm.items.first { it.size == 1 }.map { it.product.id })
    }

    @Test
    fun basketItemsOlderThanThreeDaysAreStale() {
        val now = 10 * 24 * 3_600_000L
        assertTrue(BasketViewModel.isStale(BasketItem(product("a"), addedAtMillis = now - 4 * 24 * 3_600_000L), now))
        assertFalse(BasketViewModel.isStale(BasketItem(product("a"), addedAtMillis = now - 3_600_000L), now))
    }

    @Test
    fun scoresAreSortedAndZeroesHidden() = runTest {
        profile.apply(ProfileEvent.Liked(setOf("negro")))
        profile.apply(ProfileEvent.PresetChosen(setOf("sudadera")))
        profile.apply(ProfileEvent.Liked(setOf("rojo")))
        profile.apply(ProfileEvent.Disliked(setOf("rojo")))
        profile.apply(ProfileEvent.Disliked(setOf("rojo")))
        val scores = profileVm().scores.first { it.isNotEmpty() }
        assertEquals(listOf("sudadera", "negro"), scores.map { it.first.id })
    }

    @Test
    fun ownPhotosAddTwoPoints() = runTest {
        val vm = profileVm()
        vm.addPhotos(listOf(bitmap())).join()
        assertEquals(2.0, profile.snapshot().getValue("verde"), 1e-9)
    }

    @Test
    fun clearAllEmptiesEverythingAndRestartsOnboarding() = runTest {
        profile.apply(ProfileEvent.Liked(setOf("negro")))
        basket.add(product("a"))
        settings.update { it.copy(onboardingDone = true, maxPriceEur = 100.0) }
        profileVm().clearAll().join()
        assertTrue(profile.snapshot().isEmpty())
        assertTrue(basket.items.first().isEmpty())
        assertFalse(settings.current().onboardingDone)
        assertEquals(60.0, settings.current().maxPriceEur, 1e-9)
    }

    @Test
    fun editingSizesAndPriceUpdatesSettings() = runTest {
        val vm = profileVm()
        vm.setTopSize("XL").join()
        vm.setMaxPrice(120.0).join()
        assertEquals("XL", settings.current().sizes.top)
        assertEquals(120.0, settings.current().maxPriceEur, 1e-9)
    }
}
