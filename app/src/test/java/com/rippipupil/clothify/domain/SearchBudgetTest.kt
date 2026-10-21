package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SearchBudgetTest {
    private val day = LocalDate.of(2026, 10, 21)

    private fun recordTimes(n: Int, start: BudgetState = BudgetState(), on: LocalDate = day): BudgetState {
        var s = start
        repeat(n) { s = SearchBudget.record(s, on) }
        return s
    }

    @Test
    fun freshBudgetAllowsSearch() = assertTrue(SearchBudget.canSearch(BudgetState(), day))

    @Test
    fun eightSearchesExhaustTheDay() = assertFalse(SearchBudget.canSearch(recordTimes(8), day))

    @Test
    fun nextDayResetsDailyCount() {
        val next = day.plusDays(1)
        val s = recordTimes(8)
        assertTrue(SearchBudget.canSearch(s, next))
        assertEquals(1, SearchBudget.record(s, next).dayCount)
        assertEquals(9, SearchBudget.record(s, next).monthCount)
    }

    @Test
    fun monthlyLimitBlocksOtherDays() {
        val s = BudgetState(day = "2026-10-20", dayCount = 3, month = "2026-10", monthCount = 240)
        assertFalse(SearchBudget.canSearch(s, day))
    }

    @Test
    fun newMonthResetsBoth() {
        val s = BudgetState(day = "2026-10-31", dayCount = 8, month = "2026-10", monthCount = 240)
        assertTrue(SearchBudget.canSearch(s, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun remainingIsLimitedByMonth() {
        val s = BudgetState(day = "2026-10-21", dayCount = 2, month = "2026-10", monthCount = 237)
        assertEquals(3 to 3, SearchBudget.remaining(s, day))
        assertEquals(6 to 237, SearchBudget.remaining(BudgetState(day = "2026-10-21", dayCount = 2, month = "2026-10", monthCount = 3), day))
    }
}
