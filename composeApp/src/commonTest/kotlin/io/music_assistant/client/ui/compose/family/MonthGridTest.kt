package io.music_assistant.client.ui.compose.family

import io.music_assistant.client.data.repository.FamilyEvent
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthGridTest {
    @Test
    fun everyRowIsAWholeSundayFirstWeek() {
        for (month in 1..12) {
            val grid = monthGrid(LocalDate(2026, month, 15))
            grid.forEach { week ->
                assertEquals(7, week.size)
                assertEquals(DayOfWeek.SUNDAY, week.first().dayOfWeek)
            }
            // Every day of the month appears exactly once.
            val inMonth = grid.flatten().filter { it.month.ordinal + 1 == month && it.year == 2026 }
            assertEquals(LocalDate(2026, month, 1).daysInMonth(), inMonth.size)
        }
    }

    @Test
    fun septemberStartsOnATuesdayAndFitsFiveRows() {
        val grid = monthGrid(LocalDate(2026, 9, 25))
        assertEquals(5, grid.size)
        assertEquals(LocalDate(2026, 8, 30), grid.first().first()) // Sunday before Tue Sep 1
        assertEquals(LocalDate(2026, 10, 3), grid.last().last())
    }

    @Test
    fun aMonthStartingOnSundayWithFourWeeksIsExactlyFourRows() {
        val grid = monthGrid(LocalDate(2026, 2, 10)) // Feb 1 2026 is a Sunday, 28 days
        assertEquals(4, grid.size)
        assertEquals(LocalDate(2026, 2, 1), grid.first().first())
        assertEquals(LocalDate(2026, 2, 28), grid.last().last())
    }

    @Test
    fun aMonthStartingOnSaturdayNeedsSixRows() {
        val grid = monthGrid(LocalDate(2026, 8, 5)) // Aug 1 2026 is a Saturday, 31 days
        assertEquals(6, grid.size)
        assertEquals(LocalDate(2026, 7, 26), grid.first().first())
    }

    @Test
    fun monthArithmetic() {
        assertEquals(29, LocalDate(2028, 2, 10).daysInMonth())
        assertEquals(31, LocalDate(2026, 12, 31).daysInMonth())
        assertEquals(LocalDate(2025, 12, 1), LocalDate(2026, 1, 20).plusMonths(-1))
        assertEquals(LocalDate(2027, 1, 1), LocalDate(2026, 12, 20).plusMonths(1))
        assertEquals(LocalDate(2026, 9, 1), LocalDate(2026, 9, 25).firstOfMonth())
    }

    private fun event(
        title: String,
        start: LocalDate,
        end: LocalDate = start,
        time: LocalTime? = null,
    ) = FamilyEvent("calendar.house", title, start, end, time, time, null, null)

    @Test
    fun aMultiDayStayShowsOnEachDayItCoversAndIsClippedToTheWindow() {
        val stay = event("Fall hold", LocalDate(2026, 9, 29), LocalDate(2026, 10, 13))
        val days = groupEventsByDay(listOf(stay), mapOf("calendar.house" to "House"), LocalDate(2026, 10, 1), LocalDate(2026, 10, 3))
        assertEquals(listOf(LocalDate(2026, 10, 1), LocalDate(2026, 10, 2), LocalDate(2026, 10, 3)), days.map { it.date })
        assertEquals("House", days.first().items.single().calendarName)
    }

    @Test
    fun eventsOutsideTheWindowAreDropped() {
        val before = event("Old", LocalDate(2026, 8, 1), LocalDate(2026, 8, 2))
        val after = event("Later", LocalDate(2026, 12, 1))
        assertEquals(emptyList(), groupEventsByDay(listOf(before, after), emptyMap(), LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)))
    }

    @Test
    fun allDayEventsSortBeforeTimedOnes() {
        val day = LocalDate(2026, 10, 5)
        val timed = event("Dinner", day, time = LocalTime(18, 0))
        val early = event("Coffee", day, time = LocalTime(8, 30))
        val allDay = event("Guests arrive", day)
        val days = groupEventsByDay(listOf(timed, early, allDay), emptyMap(), day, day)
        assertEquals(listOf("Guests arrive", "Coffee", "Dinner"), days.single().items.map { it.event.title })
    }
}
