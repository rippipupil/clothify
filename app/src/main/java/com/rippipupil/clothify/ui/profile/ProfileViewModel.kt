package com.rippipupil.clothify.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.settings.UserSettings
import com.rippipupil.clothify.data.style.StyleAnalyzer
import com.rippipupil.clothify.data.style.StyleResult
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.Tag
import com.rippipupil.clothify.domain.TagVocabulary
import com.rippipupil.clothify.ui.message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profile: ProfileRepository,
    private val settings: SettingsRepository,
    private val basket: BasketRepository,
    private val pool: ProductPoolRepository,
    private val analyzer: StyleAnalyzer,
    private val currentLocation: suspend () -> LatLng?,
) : ViewModel() {

    /** Etiquetas con puntuación distinta de cero, de mayor a menor. */
    val scores: StateFlow<List<Pair<Tag, Double>>> = profile.scores
        .map { map ->
            map.filterValues { it != 0.0 }
                .mapNotNull { (id, score) -> TagVocabulary.byId(id)?.let { it to score } }
                .sortedByDescending { it.second }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userSettings: StateFlow<UserSettings?> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _analyzing = MutableStateFlow(false)
    val analyzing: StateFlow<Boolean> = _analyzing

    fun setTopSize(size: String) = edit { it.copy(sizes = it.sizes.copy(top = toggle(it.sizes.top, size))) }
    fun setWaistSize(size: String) = edit { it.copy(sizes = it.sizes.copy(waist = toggle(it.sizes.waist, size))) }
    fun setShoeSize(size: String) = edit { it.copy(sizes = it.sizes.copy(shoe = toggle(it.sizes.shoe, size))) }
    fun setMaxPrice(eur: Double) = edit { it.copy(maxPriceEur = eur.coerceIn(UserSettings.MIN_PRICE, UserSettings.MAX_PRICE)) }

    fun setLocationEnabled(enabled: Boolean): Job = viewModelScope.launch {
        val location = if (enabled) currentLocation() else null
        settings.update { it.copy(locationEnabled = enabled, lastLocation = location) }
    }

    fun addPhotos(photos: List<Bitmap>): Job = viewModelScope.launch {
        _analyzing.value = true
        var added = 0
        var error: String? = null
        photos.take(MAX_PHOTOS).forEach { photo ->
            when (val result = analyzer.analyze(photo)) {
                is StyleResult.Success -> {
                    profile.apply(ProfileEvent.OwnPhoto(result.tagIds.toSet()))
                    added++
                }
                is StyleResult.Failure -> error = result.reason.message()
            }
        }
        _analyzing.value = false
        _message.value = error ?: "Añadidas $added fotos a tu estilo"
    }

    fun resetSeen(): Job = viewModelScope.launch {
        pool.resetSeen()
        _message.value = "Las prendas vistas volverán a aparecer"
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun clearAll(): Job = viewModelScope.launch {
        profile.clear()
        basket.clear()
        pool.clearAll()
        settings.clear()
    }

    private fun edit(transform: (UserSettings) -> UserSettings): Job = viewModelScope.launch { settings.update(transform) }

    private fun toggle(current: String?, value: String) = if (current == value) null else value

    private companion object {
        const val MAX_PHOTOS = 5
    }
}
