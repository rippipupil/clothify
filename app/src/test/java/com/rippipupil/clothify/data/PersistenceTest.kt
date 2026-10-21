package com.rippipupil.clothify.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.local.SeenProductEntity
import com.rippipupil.clothify.data.local.toEntity
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.domain.BudgetState
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.UserSizes
import com.rippipupil.clothify.domain.product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class PersistenceTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: ClothifyDatabase

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun likeIsReflectedInSnapshot() = runTest {
        val profile = ProfileRepository(db)
        profile.apply(ProfileEvent.Liked(setOf("sudadera")))
        profile.apply(ProfileEvent.Liked(setOf("sudadera", "negro")))
        assertEquals(mapOf("sudadera" to 2.0, "negro" to 1.0), profile.snapshot())
        assertEquals(profile.snapshot(), profile.scores.first())
    }

    @Test
    fun basketAddAndRemove() = runTest {
        val basket = BasketRepository(db) { 1000L }
        basket.add(product("a", setOf("sudadera")))
        basket.add(product("b"))
        assertEquals(setOf("a", "b"), basket.items.first().map { it.product.id }.toSet())
        assertEquals(setOf("sudadera"), basket.items.first().first { it.product.id == "a" }.product.tagIds)
        basket.remove("a")
        assertEquals(listOf("b"), basket.items.first().map { it.product.id })
    }

    @Test
    fun unseenFreshExcludesSeenAndExpired() = runTest {
        val dao = db.productDao()
        dao.upsertAll(listOf(product("old", fetchedAt = 10), product("seen", fetchedAt = 500), product("ok", fetchedAt = 500)).map { it.toEntity() })
        db.seenDao().insert(SeenProductEntity("seen", liked = true, seenAtMillis = 600))
        assertEquals(listOf("ok"), dao.unseenFresh(minFetchedAt = 100).map { it.id })
        assertEquals(1, dao.countUnseenFresh(minFetchedAt = 100))
        dao.deleteOlderThan(100)
        assertEquals(setOf("seen", "ok"), dao.all().map { it.id }.toSet())
    }

    @Test
    fun settingsRoundTrip() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            tmp.newFile("settings.preferences_pb")
        }
        val settings = SettingsRepository(store)
        assertEquals(60.0, settings.settings.first().maxPriceEur, 1e-9)
        val budget = BudgetState("2026-10-21", 2, "2026-10", 9)
        settings.update { it.copy(maxPriceEur = 45.0, budget = budget, sizes = UserSizes(top = "L"), onboardingDone = true, priceAlertsEnabled = true) }
        val read = settings.settings.first()
        assertEquals(45.0, read.maxPriceEur, 1e-9)
        assertEquals(budget, read.budget)
        assertEquals("L", read.sizes.top)
        assertTrue(read.onboardingDone)
        assertTrue(read.priceAlertsEnabled)
    }
}
