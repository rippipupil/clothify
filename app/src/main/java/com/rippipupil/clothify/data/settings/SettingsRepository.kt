package com.rippipupil.clothify.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rippipupil.clothify.domain.BudgetState
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.UserSizes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserSettings(
    val onboardingDone: Boolean = false,
    val sizes: UserSizes = UserSizes(),
    val maxPriceEur: Double = DEFAULT_MAX_PRICE,
    val locationEnabled: Boolean = false,
    val lastLocation: LatLng? = null,
    val budget: BudgetState = BudgetState(),
) {
    companion object {
        const val DEFAULT_MAX_PRICE = 60.0
        const val MIN_PRICE = 10.0
        const val MAX_PRICE = 300.0
    }
}

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<UserSettings> = store.data.map { it.toSettings() }

    suspend fun current(): UserSettings = settings.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private fun Preferences.toSettings(): UserSettings {
        val lat = this[LAT]
        val lon = this[LON]
        return UserSettings(
            onboardingDone = this[ONBOARDING_DONE] ?: false,
            sizes = UserSizes(top = this[TOP_SIZE], waist = this[WAIST_SIZE], shoe = this[SHOE_SIZE]),
            maxPriceEur = this[MAX_PRICE] ?: UserSettings.DEFAULT_MAX_PRICE,
            locationEnabled = this[LOCATION_ENABLED] ?: false,
            lastLocation = if (lat != null && lon != null) LatLng(lat, lon) else null,
            budget = BudgetState(
                day = this[BUDGET_DAY] ?: "",
                dayCount = this[BUDGET_DAY_COUNT] ?: 0,
                month = this[BUDGET_MONTH] ?: "",
                monthCount = this[BUDGET_MONTH_COUNT] ?: 0,
            ),
        )
    }

    private fun MutablePreferences.write(s: UserSettings) {
        this[ONBOARDING_DONE] = s.onboardingDone
        setOrRemove(TOP_SIZE, s.sizes.top)
        setOrRemove(WAIST_SIZE, s.sizes.waist)
        setOrRemove(SHOE_SIZE, s.sizes.shoe)
        this[MAX_PRICE] = s.maxPriceEur
        this[LOCATION_ENABLED] = s.locationEnabled
        setOrRemove(LAT, s.lastLocation?.lat)
        setOrRemove(LON, s.lastLocation?.lon)
        this[BUDGET_DAY] = s.budget.day
        this[BUDGET_DAY_COUNT] = s.budget.dayCount
        this[BUDGET_MONTH] = s.budget.month
        this[BUDGET_MONTH_COUNT] = s.budget.monthCount
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val TOP_SIZE = stringPreferencesKey("top_size")
        val WAIST_SIZE = stringPreferencesKey("waist_size")
        val SHOE_SIZE = stringPreferencesKey("shoe_size")
        val MAX_PRICE = doublePreferencesKey("max_price_eur")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val LAT = doublePreferencesKey("last_lat")
        val LON = doublePreferencesKey("last_lon")
        val BUDGET_DAY = stringPreferencesKey("budget_day")
        val BUDGET_DAY_COUNT = intPreferencesKey("budget_day_count")
        val BUDGET_MONTH = stringPreferencesKey("budget_month")
        val BUDGET_MONTH_COUNT = intPreferencesKey("budget_month_count")
    }
}
