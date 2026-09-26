package io.music_assistant.client.ui.compose.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.music_assistant.client.data.repository.FamilyCalendar
import io.music_assistant.client.data.repository.FamilyCalendarRepository
import io.music_assistant.client.data.repository.FamilyEvent
import io.music_assistant.client.data.repository.FamilyLoginResult
import io.music_assistant.client.data.repository.FamilySessionExpiredException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** One event as shown on a particular day of the agenda (multi-day events appear on each day). */
data class AgendaItem(val event: FamilyEvent, val calendarName: String)

data class AgendaDay(val date: LocalDate, val items: List<AgendaItem>)

data class FamilyCalendarState(
    val signedIn: Boolean,
    val server: String? = null,
    /** A sign-in request is in flight. */
    val busy: Boolean = false,
    val error: String? = null,
    /** The account has two-factor login on and needs its 6-digit code. */
    val needsCode: Boolean = false,
    val loading: Boolean = false,
    val loadError: String? = null,
    val calendars: List<FamilyCalendar> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val days: List<AgendaDay> = emptyList(),
    val today: LocalDate? = null,
    /** The calendar new stays get added to; null if none of the shown calendars accepts events. */
    val scheduleCalendar: FamilyCalendar? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Set after a successful save so the dialog can close; cleared by [FamilyCalendarViewModel.clearSaveStatus]. */
    val saved: Boolean = false,
    val view: CalendarView = CalendarView.Agenda,
    /** First day of the month shown in the month view. */
    val month: LocalDate? = null,
    val monthDays: List<AgendaDay> = emptyList(),
    val monthLoading: Boolean = false,
    val selectedDay: LocalDate? = null,
)

enum class CalendarView { Agenda, Month }

/** How far ahead the agenda looks. */
private const val WINDOW_DAYS = 90
private const val KEY_HIDDEN_CALENDARS = "family_calendar_hidden_ids"

class FamilyCalendarViewModel(
    private val repository: FamilyCalendarRepository,
    private val settings: Settings,
) : ViewModel() {
    private val _state = MutableStateFlow(
        FamilyCalendarState(signedIn = repository.isSignedIn, server = repository.serverLabel),
    )
    val state = _state.asStateFlow()

    init {
        if (repository.isSignedIn) refresh()
    }

    fun signIn(address: String, username: String, password: String) {
        if (username.isBlank() || password.isEmpty()) {
            _state.update { it.copy(error = "Enter your username and password.") }
            return
        }
        launchLogin { repository.signIn(address, username, password) }
    }

    fun submitCode(code: String) {
        if (code.isBlank()) {
            _state.update { it.copy(error = "Enter the 6-digit code from your authenticator app.") }
            return
        }
        launchLogin { repository.submitCode(code) }
    }

    private fun launchLogin(attempt: suspend () -> FamilyLoginResult) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = attempt()) {
                FamilyLoginResult.Success -> {
                    _state.update {
                        it.copy(
                            signedIn = true,
                            server = repository.serverLabel,
                            busy = false,
                            needsCode = false,
                            error = null,
                        )
                    }
                    refresh()
                }

                FamilyLoginResult.NeedsCode -> _state.update { it.copy(busy = false, needsCode = true) }
                is FamilyLoginResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val calendars = repository.calendars()
                val hidden = settings.getString(KEY_HIDDEN_CALENDARS, "").split(',').filter { it.isNotBlank() }.toSet()
                val selected = calendars.map { it.entityId }.filter { it !in hidden }.toSet()
                _state.update {
                    it.copy(calendars = calendars, selectedIds = selected, scheduleCalendar = calendars.firstOrNull { c -> c.canCreate })
                }
                loadEvents(calendars, selected)
                if (_state.value.view == CalendarView.Month) loadMonth()
            } catch (e: FamilySessionExpiredException) {
                signedOutWithMessage("Your sign-in expired. Please sign in again.")
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, loadError = "Couldn't load the calendar. Check your connection and try again.")
                }
            }
        }
    }

    // ---- Month view ----

    fun setView(view: CalendarView) {
        if (_state.value.view == view) return
        _state.update { it.copy(view = view) }
        if (view == CalendarView.Month && _state.value.month == null) {
            val today = _state.value.today ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            showMonth(today.firstOfMonth())
        }
    }

    /** Moves the month view to [month] (any day in it) and loads its events. */
    fun showMonth(month: LocalDate) {
        val first = month.firstOfMonth()
        val today = _state.value.today ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        // Land on today if it's in this month, otherwise the 1st.
        val selected = if (today.firstOfMonth() == first) today else first
        _state.update { it.copy(month = first, selectedDay = selected, monthLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                loadMonth()
            } catch (e: FamilySessionExpiredException) {
                signedOutWithMessage("Your sign-in expired. Please sign in again.")
            } catch (e: Exception) {
                _state.update {
                    it.copy(monthLoading = false, loadError = "Couldn't load that month. Check your connection and try again.")
                }
            }
        }
    }

    fun changeMonth(delta: Int) {
        val current = _state.value.month ?: return
        showMonth(current.plusMonths(delta))
    }

    fun selectDay(day: LocalDate) {
        _state.update { it.copy(selectedDay = day) }
    }

    /** Fetches every event visible in the month grid (including the neighbouring days shown). */
    private suspend fun loadMonth() {
        val current = _state.value
        val month = current.month ?: return
        val grid = monthGrid(month)
        val from = grid.first().first()
        val to = grid.last().last()
        val names = current.calendars.associate { it.entityId to it.name }
        val events = kotlinx.coroutines.coroutineScope {
            current.selectedIds.map { id -> async { repository.events(id, from, to) } }.awaitAll().flatten()
        }
        // Ignore a slow response if the person has already moved to another month.
        if (_state.value.month != month) return
        _state.update { it.copy(monthLoading = false, monthDays = groupEventsByDay(events, names, from, to)) }
    }

    fun toggleCalendar(entityId: String) {
        val current = _state.value
        val selected = if (entityId in current.selectedIds) current.selectedIds - entityId else current.selectedIds + entityId
        val hidden = current.calendars.map { it.entityId }.filter { it !in selected }
        settings.putString(KEY_HIDDEN_CALENDARS, hidden.joinToString(","))
        _state.update { it.copy(selectedIds = selected, loading = true, loadError = null) }
        viewModelScope.launch {
            try {
                loadEvents(current.calendars, selected)
                if (_state.value.view == CalendarView.Month) loadMonth()
            } catch (e: FamilySessionExpiredException) {
                signedOutWithMessage("Your sign-in expired. Please sign in again.")
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = "Couldn't load the calendar. Try again.") }
            }
        }
    }

    /** Adds an all-day block (a stay, a hold, a party) covering [firstDay]..[lastDay] inclusive. */
    fun schedule(title: String, firstDay: LocalDate, lastDay: LocalDate, notes: String) {
        val current = _state.value
        val calendar = current.scheduleCalendar ?: return
        val error = when {
            title.isBlank() -> "Give it a name, like \"Sam's family\"."
            lastDay < firstDay -> "The last day can't be before the first day."
            current.today != null && lastDay < current.today -> "That date has already passed."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(saveError = error) }
            return
        }
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                repository.createAllDayEvent(calendar.entityId, title, firstDay, lastDay, notes)
                _state.update { it.copy(saving = false, saved = true) }
                refresh()
            } catch (e: FamilySessionExpiredException) {
                signedOutWithMessage("Your sign-in expired. Please sign in again.")
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, saveError = "Couldn't save that. Check your connection and try again.")
                }
            }
        }
    }

    fun clearSaveStatus() {
        _state.update { it.copy(saving = false, saveError = null, saved = false) }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            signedOutWithMessage(null)
        }
    }

    private fun signedOutWithMessage(message: String?) {
        _state.value = FamilyCalendarState(signedIn = false, error = message)
    }

    private suspend fun loadEvents(calendars: List<FamilyCalendar>, selected: Set<String>) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val windowEnd = today.plus(WINDOW_DAYS, DateTimeUnit.DAY)
        val names = calendars.associate { it.entityId to it.name }

        val events = kotlinx.coroutines.coroutineScope {
            selected.map { id -> async { repository.events(id, today, windowEnd) } }.awaitAll().flatten()
        }

        val days = groupEventsByDay(events, names, today, windowEnd)
        _state.update { it.copy(loading = false, loadError = null, days = days, today = today) }
    }
}
