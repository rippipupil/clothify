package com.rippipupil.clothify.ui

import androidx.test.core.app.ApplicationProvider
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.sources.ProductSource
import com.rippipupil.clothify.data.sources.SourceFailure
import com.rippipupil.clothify.data.sources.SourceResult
import com.rippipupil.clothify.domain.FixedRandom
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.QueryPlanner
import com.rippipupil.clothify.domain.Recommender
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.product
import com.rippipupil.clothify.ui.discover.DiscoverUiState
import com.rippipupil.clothify.ui.discover.DiscoverViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class DiscoverViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: ClothifyDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var profile: ProfileRepository
    private lateinit var basket: BasketRepository

    private class Fake(override val source: Source, val perQuery: Int, val fail: SourceFailure? = null) : ProductSource {
        override suspend fun search(query: SearchQuery): SourceResult =
            if (fail != null) SourceResult.Failed(fail)
            else SourceResult.Ok(List(perQuery) { product("${query.text}-$it", query.tagIds + "sudadera", source, fetchedAt = System.currentTimeMillis()) })
    }

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
        settings = testSettings(tmp)
        profile = ProfileRepository(db)
        basket = BasketRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun vm(vararg sources: ProductSource): DiscoverViewModel {
        val pool = ProductPoolRepository(db, sources.toList(), QueryPlanner(Random(3)), profile, settings)
        return DiscoverViewModel(pool, profile, basket, Recommender(FixedRandom(0.5)))
    }

    private suspend fun DiscoverViewModel.cards(): DiscoverUiState.Cards =
        state.first { it is DiscoverUiState.Cards } as DiscoverUiState.Cards

    @Test
    fun loadsAStackOfThreeCards() = runTest {
        val vm = vm(Fake(Source.WALLAPOP, 10))
        assertEquals(3, vm.cards().stack.size)
    }

    @Test
    fun swipeRightRaisesScoresAndRemovesTheCard() = runTest {
        val vm = vm(Fake(Source.WALLAPOP, 10))
        val top = vm.cards().stack.first()
        vm.onSwipe(top, liked = true).join()
        assertEquals(1.0, profile.snapshot().getValue("sudadera"), 1e-9)
        assertTrue(vm.cards().stack.none { it.id == top.id })
        assertEquals(3, vm.cards().stack.size)
    }

    @Test
    fun swipeLeftLowersScores() = runTest {
        profile.apply(ProfileEvent.Liked(setOf("sudadera")))
        val vm = vm(Fake(Source.WALLAPOP, 10))
        vm.onSwipe(vm.cards().stack.first(), liked = false).join()
        assertEquals(0.5, profile.snapshot().getValue("sudadera"), 1e-9)
    }

    @Test
    fun basketButtonKeepsTheCard() = runTest {
        val vm = vm(Fake(Source.WALLAPOP, 10))
        val top = vm.cards().stack.first()
        vm.onAddToBasket(top).join()
        assertEquals(listOf(top.id), basket.items.first().map { it.product.id })
        assertEquals(top.id, vm.cards().stack.first().id)
    }

    @Test
    fun allSourcesFailingGivesEmptyWithWarning() = runTest {
        val vm = vm(Fake(Source.WALLAPOP, 0, SourceFailure.BLOCKED), Fake(Source.VINTED, 0, SourceFailure.BAD_RESPONSE))
        val empty = vm.state.first { it is DiscoverUiState.Empty } as DiscoverUiState.Empty
        assertNotNull(empty.warning)
    }
}
