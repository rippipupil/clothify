package com.rippipupil.clothify.ui.onboarding

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.settings.UserSettings
import com.rippipupil.clothify.data.style.StyleAnalyzer
import com.rippipupil.clothify.data.style.StyleResult
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.StylePresets
import com.rippipupil.clothify.domain.UserSizes
import com.rippipupil.clothify.ui.message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val step: Int = 0,
    val sizes: UserSizes = UserSizes(),
    val maxPriceEur: Double = UserSettings.DEFAULT_MAX_PRICE,
    val locationEnabled: Boolean = false,
    val location: LatLng? = null,
    val selectedPresets: Set<String> = emptySet(),
    val photoTags: List<Set<String>> = emptyList(),
    val analyzing: Boolean = false,
    val message: String? = null,
) {
    val analyzedPhotos: Int get() = photoTags.size
    val canFinish: Boolean get() = selectedPresets.isNotEmpty() && !analyzing
}

class OnboardingViewModel(
    private val profile: ProfileRepository,
    private val settings: SettingsRepository,
    private val analyzer: StyleAnalyzer,
    private val startRefill: () -> Unit,
    private val currentLocation: suspend () -> LatLng?,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP)) }
    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun setTopSize(size: String) = _state.update { it.copy(sizes = it.sizes.copy(top = toggle(it.sizes.top, size))) }
    fun setWaistSize(size: String) = _state.update { it.copy(sizes = it.sizes.copy(waist = toggle(it.sizes.waist, size))) }
    fun setShoeSize(size: String) = _state.update { it.copy(sizes = it.sizes.copy(shoe = toggle(it.sizes.shoe, size))) }
    fun setMaxPrice(eur: Double) = _state.update {
        it.copy(maxPriceEur = eur.coerceIn(UserSettings.MIN_PRICE, UserSettings.MAX_PRICE))
    }

    fun togglePreset(id: String) = _state.update {
        it.copy(selectedPresets = if (id in it.selectedPresets) it.selectedPresets - id else it.selectedPresets + id)
    }

    fun onLocationPermission(granted: Boolean): Job = viewModelScope.launch {
        val location = if (granted) currentLocation() else null
        _state.update { it.copy(locationEnabled = granted, location = location) }
    }

    fun analyzePhotos(photos: List<Bitmap>): Job = viewModelScope.launch {
        _state.update { it.copy(analyzing = true, message = null) }
        var lastError: String? = null
        for (photo in photos.take(MAX_PHOTOS)) {
            when (val result = analyzer.analyze(photo)) {
                is StyleResult.Success -> _state.update { it.copy(photoTags = it.photoTags + setOf(result.tagIds.toSet())) }
                is StyleResult.Failure -> lastError = result.reason.message()
            }
        }
        _state.update { it.copy(analyzing = false, message = lastError) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun finish(): Job = viewModelScope.launch {
        val s = _state.value
        StylePresets.all.filter { it.id in s.selectedPresets }.forEach { profile.apply(ProfileEvent.PresetChosen(it.tagIds)) }
        s.photoTags.forEach { profile.apply(ProfileEvent.OwnPhoto(it)) }
        settings.update {
            it.copy(
                onboardingDone = true,
                sizes = s.sizes,
                maxPriceEur = s.maxPriceEur,
                locationEnabled = s.locationEnabled,
                lastLocation = s.location,
            )
        }
        startRefill()
        // Si más adelante se borran los datos, la bienvenida vuelve a empezar de cero.
        _state.value = OnboardingState()
    }

    private fun toggle(current: String?, value: String) = if (current == value) null else value

    companion object {
        const val LAST_STEP = 2
        const val MAX_PHOTOS = 5
    }
}
