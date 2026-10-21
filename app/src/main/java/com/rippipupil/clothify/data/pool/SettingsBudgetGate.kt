package com.rippipupil.clothify.data.pool

import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.sources.BudgetGate
import com.rippipupil.clothify.domain.SearchBudget
import java.time.LocalDate

/** Presupuesto de SerpApi guardado en los ajustes. */
class SettingsBudgetGate(
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : BudgetGate {
    override suspend fun tryConsume(): Boolean {
        var allowed = false
        settings.update { s ->
            val day = today()
            allowed = SearchBudget.canSearch(s.budget, day)
            if (allowed) s.copy(budget = SearchBudget.record(s.budget, day)) else s
        }
        return allowed
    }
}
