package io.music_assistant.client.ui.compose.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

/**
 * Family-only calendar, backed by the family's own Home Assistant. Signed out it shows a sign-in
 * form; signed in it shows the next 90 days as an agenda. Nothing about the calendar (or the
 * server it comes from) is in the app itself — see [io.music_assistant.client.data.repository.FamilyCalendarRepository].
 */
@Composable
fun FamilyCalendarScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: FamilyCalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // contentPadding never carries a top value, so the status bar inset is added separately.
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + contentPadding.calculateTopPadding()
    val bottom = contentPadding.calculateBottomPadding()

    if (state.signedIn) {
        CalendarContent(state, viewModel, top, bottom)
    } else {
        SignInContent(state, viewModel, top, bottom)
    }
}

@Composable
private fun SignInContent(
    state: FamilyCalendarState,
    viewModel: FamilyCalendarViewModel,
    top: androidx.compose.ui.unit.Dp,
    bottom: androidx.compose.ui.unit.Dp,
) {
    var address by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = top + 24.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Family Calendar",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "For family only. Sign in with the Home Assistant account you were given. " +
                "Guests don't need this — everything else in the app works without it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.needsCode) {
            Text(
                text = "Enter the 6-digit code from your authenticator app.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(8) },
                label = { Text("Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Home Assistant address") },
                placeholder = { Text("example.ui.nabu.casa") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.error?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (state.needsCode) viewModel.submitCode(code) else viewModel.signIn(address, username, password)
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp)
            } else {
                Text(if (state.needsCode) "Verify" else "Sign in")
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: FamilyCalendarState,
    viewModel: FamilyCalendarViewModel,
    top: androidx.compose.ui.unit.Dp,
    bottom: androidx.compose.ui.unit.Dp,
) {
    var showSchedule by remember { mutableStateOf(false) }
    if (showSchedule) {
        ScheduleDialog(
            state = state,
            initialDay = if (state.view == CalendarView.Month) state.selectedDay else null,
            onSave = viewModel::schedule,
            onDismiss = {
                showSchedule = false
                viewModel.clearSaveStatus()
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = bottom + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Family Calendar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    state.server?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
                TextButton(onClick = viewModel::signOut) { Text("Sign out") }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.view == CalendarView.Agenda,
                    onClick = { viewModel.setView(CalendarView.Agenda) },
                    label = { Text("List") },
                )
                FilterChip(
                    selected = state.view == CalendarView.Month,
                    onClick = { viewModel.setView(CalendarView.Month) },
                    label = { Text("Month") },
                )
            }
        }

        state.scheduleCalendar?.let {
            item {
                Button(onClick = { showSchedule = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Schedule the house")
                }
            }
        }

        if (state.calendars.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.calendars.forEach { calendar ->
                        FilterChip(
                            selected = calendar.entityId in state.selectedIds,
                            onClick = { viewModel.toggleCalendar(calendar.entityId) },
                            label = { Text(calendar.name) },
                        )
                    }
                }
            }
        }

        if (state.view == CalendarView.Month) {
            monthItems(state, viewModel, onSchedule = { showSchedule = true })
        } else {
            if (state.loading && state.days.isEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.loadError?.let { message ->
                item {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            if (!state.loading && state.loadError == null && state.days.isEmpty()) {
                item {
                    Text(
                        text = "Nothing on the calendar for the next 90 days.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }

            state.days.forEach { day ->
                item(key = "header-${day.date}") {
                    Text(
                        text = dayLabel(day.date, state.today),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(day.items, key = { "${day.date}-${it.event.calendarId}-${it.event.title}-${it.event.startTime}" }) { item ->
                    EventCard(item, showCalendar = state.calendars.size > 1)
                }
            }

        }
    }
}

@Composable
private fun EventCard(item: AgendaItem, showCalendar: Boolean) {
    val event = item.event
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = timeLabel(event.startTime, event.endTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        event.location?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        event.description?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showCalendar && item.calendarName.isNotBlank()) {
            Text(text = item.calendarName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun timeLabel(start: LocalTime?, end: LocalTime?): String = when {
    start == null -> "All day"
    end == null || end == start -> formatTime(start)
    else -> "${formatTime(start)} – ${formatTime(end)}"
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    val minute = time.minute.toString().padStart(2, '0')
    return "$hour:$minute ${if (time.hour < 12) "AM" else "PM"}"
}

private fun dayLabel(date: LocalDate, today: LocalDate?): String = when (date) {
    today -> "Today"
    today?.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
    else -> "${date.dayOfWeek.name.title().take(3)}, ${date.month.name.title().take(3)} ${date.day}"
}

private fun String.title() = lowercase().replaceFirstChar { it.uppercase() }

/**
 * "Schedule the house": name plus a first and last day. Warns — but doesn't block — when the
 * days overlap something already on the calendar, since family can sort out a double-booking
 * between themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(
    state: FamilyCalendarState,
    initialDay: LocalDate?,
    onSave: (title: String, firstDay: LocalDate, lastDay: LocalDate, notes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = state.today ?: return
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var firstDay by remember { mutableStateOf(initialDay ?: today) }
    var lastDay by remember { mutableStateOf(initialDay ?: today) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }

    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }

    val conflicts = (state.days + state.monthDays)
        .filter { it.date in firstDay..lastDay }
        .flatMap { it.items }
        .map { it.event.title }
        .distinct()

    AlertDialog(
        onDismissRequest = { if (!state.saving) onDismiss() },
        title = { Text("Schedule the house") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Who / what") },
                    placeholder = { Text("Sam's family") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { picking = PickTarget.First }, modifier = Modifier.weight(1f)) {
                        Text("From ${shortDate(firstDay)}")
                    }
                    OutlinedButton(onClick = { picking = PickTarget.Last }, modifier = Modifier.weight(1f)) {
                        Text("To ${shortDate(lastDay)}")
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (conflicts.isNotEmpty()) {
                    Text(
                        text = "Heads up — already on the calendar for those days: ${conflicts.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.saveError?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, firstDay, lastDay, notes) }, enabled = !state.saving) {
                if (state.saving) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Add to calendar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.saving) { Text("Cancel") } },
    )

    picking?.let { target ->
        val initial = if (target == PickTarget.First) firstDay else lastDay
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                            if (target == PickTarget.First) {
                                firstDay = date
                                if (lastDay < date) lastDay = date
                            } else {
                                lastDay = date
                                if (firstDay > date) firstDay = date
                            }
                        }
                        picking = null
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class PickTarget { First, Last }

private fun shortDate(date: LocalDate) = "${date.month.name.title().take(3)} ${date.day}"

// ---- Month view ----

private val weekdayInitials = listOf("S", "M", "T", "W", "T", "F", "S")

private fun LazyListScope.monthItems(
    state: FamilyCalendarState,
    viewModel: FamilyCalendarViewModel,
    onSchedule: () -> Unit,
) {
    val month = state.month ?: return
    val today = state.today
    val byDate = state.monthDays.associate { it.date to it.items }

    item(key = "month-nav") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.changeMonth(-1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = "${month.month.name.title()} ${month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.changeMonth(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }
    }

    item(key = "month-grid") {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayInitials.forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            monthGrid(month).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            inMonth = date.month == month.month,
                            isToday = date == today,
                            isSelected = date == state.selectedDay,
                            items = byDate[date].orEmpty(),
                            onClick = { viewModel.selectDay(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (state.monthLoading) {
        item(key = "month-loading") {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
            }
        }
    }

    state.loadError?.let { message ->
        item(key = "month-error") {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }

    val selected = state.selectedDay
    if (selected != null) {
        val items = byDate[selected].orEmpty()
        item(key = "month-selected-header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "${dayLabel(selected, today)} · ${selected.month.name.title().take(3)} ${selected.day}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (state.scheduleCalendar != null) {
                    TextButton(onClick = onSchedule) { Text("Schedule this day") }
                }
            }
        }
        if (items.isEmpty() && !state.monthLoading) {
            item(key = "month-selected-empty") {
                Text(
                    text = "Nothing scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items, key = { "m-$selected-${it.event.calendarId}-${it.event.title}-${it.event.startTime}" }) { item ->
            EventCard(item, showCalendar = state.calendars.size > 1)
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    items: List<AgendaItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (inMonth) scheme.surfaceContainerLow else Color.Transparent)
            .then(if (isSelected) Modifier.border(BorderStroke(2.dp, scheme.primary), RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isToday) scheme.primary else Color.Transparent)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = "${date.day}",
                fontSize = 12.sp,
                color = when {
                    isToday -> scheme.onPrimary
                    inMonth -> scheme.onSurface
                    else -> scheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
            )
        }
        items.take(2).forEach { item ->
            Text(
                text = item.event.title,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                color = scheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(scheme.primaryContainer)
                    .padding(horizontal = 2.dp),
            )
        }
        if (items.size > 2) {
            Text(text = "+${items.size - 2}", fontSize = 9.sp, color = scheme.onSurfaceVariant)
        }
    }
}
