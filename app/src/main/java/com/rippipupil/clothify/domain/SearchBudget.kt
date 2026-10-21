package com.rippipupil.clothify.domain

import java.time.LocalDate

/** Búsquedas de SerpApi gastadas: día "2026-10-21", mes "2026-10". */
data class BudgetState(val day: String = "", val dayCount: Int = 0, val month: String = "", val monthCount: Int = 0)

/** Límites para no pasar del plan gratuito de SerpApi (250/mes). */
object SearchBudget {
    const val DAILY = 8
    const val MONTHLY = 240

    fun canSearch(state: BudgetState, today: LocalDate): Boolean = remaining(state, today).first > 0

    fun record(state: BudgetState, today: LocalDate): BudgetState =
        rolled(state, today).let { it.copy(dayCount = it.dayCount + 1, monthCount = it.monthCount + 1) }

    /** (restantes hoy, restantes este mes). */
    fun remaining(state: BudgetState, today: LocalDate): Pair<Int, Int> {
        val s = rolled(state, today)
        val month = (MONTHLY - s.monthCount).coerceAtLeast(0)
        val day = (DAILY - s.dayCount).coerceAtLeast(0).coerceAtMost(month)
        return day to month
    }

    private fun rolled(state: BudgetState, today: LocalDate): BudgetState {
        val day = today.toString()
        val month = day.substring(0, 7)
        return BudgetState(
            day = day,
            dayCount = if (state.day == day) state.dayCount else 0,
            month = month,
            monthCount = if (state.month == month) state.monthCount else 0,
        )
    }
}
