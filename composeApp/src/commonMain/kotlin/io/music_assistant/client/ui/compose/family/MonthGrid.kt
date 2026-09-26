package io.music_assistant.client.ui.compose.family

import io.music_assistant.client.data.repository.FamilyEvent
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** The first day of the month containing [this]. */
fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

fun LocalDate.plusMonths(months: Int): LocalDate = firstOfMonth().plus(months, DateTimeUnit.MONTH)

/** Number of days in the month containing [this]. */
fun LocalDate.daysInMonth(): Int {
    val first = firstOfMonth()
    return first.daysUntil(first.plus(1, DateTimeUnit.MONTH))
}

/**
 * The grid for a month view: whole weeks, Sunday first, covering every day of the month
 * containing [anyDayInMonth]. The first and last rows include trailing days of the neighbouring
 * months so every row has 7 cells.
 */
fun monthGrid(anyDayInMonth: LocalDate): List<List<LocalDate>> {
    val first = anyDayInMonth.firstOfMonth()
    // kotlinx DayOfWeek: Monday = 0 … Sunday = 6, so Sunday-first offset is (ordinal + 1) % 7.
    val leading = (first.dayOfWeek.ordinal + 1) % 7
    val start = first.plus(-leading, DateTimeUnit.DAY)
    val weeks = (leading + first.daysInMonth() + 6) / 7
    return List(weeks) { w -> List(7) { d -> start.plus(w * 7 + d, DateTimeUnit.DAY) } }
}

/**
 * Spreads events across the days they cover, limited to [from]..[to], so a multi-day stay shows
 * on every day of the stay. All-day events sort before timed ones, then by start time.
 */
fun groupEventsByDay(
    events: List<FamilyEvent>,
    calendarNames: Map<String, String>,
    from: LocalDate,
    to: LocalDate,
): List<AgendaDay> {
    val perDay = mutableListOf<Pair<LocalDate, AgendaItem>>()
    for (event in events) {
        var day = if (event.startDate > from) event.startDate else from
        val last = if (event.endDate < to) event.endDate else to
        while (day <= last) {
            perDay += day to AgendaItem(event, calendarNames[event.calendarId] ?: "")
            day = day.plus(1, DateTimeUnit.DAY)
        }
    }
    return perDay.groupBy({ it.first }, { it.second })
        .toList()
        .sortedBy { it.first }
        .map { (date, items) ->
            AgendaDay(date, items.sortedWith(compareBy({ it.event.startTime != null }, { it.event.startTime })))
        }
}
