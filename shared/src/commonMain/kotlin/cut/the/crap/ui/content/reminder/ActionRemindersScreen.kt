@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.ui.content.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.platform.rememberNotificationPermissionRequester
import cut.the.crap.shared.resources.*
import cut.the.crap.tools.formatMediumDateForLanguage
import cut.the.crap.tools.formatMediumDateTime
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The action reminder manager: list, create, enable/disable, send now, delete (spec §2.1).
 * Reached from the campaign detail screen and from a reminder notification's tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionRemindersScreen(
    navController: NavHostController,
    viewModel: ActionRemindersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var creating by rememberSaveable { mutableStateOf(false) }
    val permissionRequester = rememberNotificationPermissionRequester {
        viewModel.refreshNotificationPermission()
    }

    // Back from the permission dialog or the settings page: re-read what is allowed now.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshNotificationPermission()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            // Launched, not inline: showSnackbar suspends until dismissed, and the next event
            // (say, a second delete) must not wait for it.
            when (event) {
                is ActionRemindersEvent.Deleted -> launch {
                    val result = snackbarHostState.showSnackbar(
                        message = getString(Res.string.reminders_deleted),
                        actionLabel = getString(Res.string.reminders_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.reminder)
                }
                ActionRemindersEvent.Sent -> launch {
                    snackbarHostState.showSnackbar(getString(Res.string.reminders_sent))
                }
                ActionRemindersEvent.NothingToSend -> launch {
                    snackbarHostState.showSnackbar(getString(Res.string.reminders_nothing_to_send))
                }
                ActionRemindersEvent.RequestNotificationPermission -> permissionRequester.request()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.reminders_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // New reminders are made from the campaign's posts, so they need it loaded.
            if (state.campaign != null) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.reminders_add)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.campaignLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.canNotify) {
                    item {
                        NotificationsOffBanner(
                            onAllow = permissionRequester::request,
                            onOpenSettings = viewModel::openNotificationSettings,
                        )
                    }
                }
                if (state.campaign == null && state.campaignError != null) {
                    item {
                        CampaignErrorRow(onRetry = viewModel::loadCampaign)
                    }
                }
                if (state.rows.isEmpty() && !state.campaignLoading) {
                    item {
                        Text(
                            text = stringResource(Res.string.reminders_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.rows, key = { it.reminder.id }) { row ->
                    ReminderCard(
                        row = row,
                        onEnabledChange = { viewModel.setEnabled(row.reminder.id, it) },
                        onSendNow = { viewModel.sendNow(row.reminder.id) },
                        onDelete = { viewModel.delete(row.reminder) },
                    )
                }
            }
        }
    }

    val campaign = state.campaign
    if (creating && campaign != null) {
        CreateReminderSheet(
            campaign = campaign,
            onDismiss = { creating = false },
            onSave = { draft -> viewModel.create(draft).also { if (it == null) creating = false } },
        )
    }
}

@Composable
private fun NotificationsOffBanner(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.reminders_notifications_off),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            // Two ways out: once the user has declined twice Android no longer shows the
            // dialog, and only the settings page can turn notifications back on.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(Res.string.reminders_notifications_settings))
                }
                TextButton(onClick = onAllow) {
                    Text(stringResource(Res.string.reminders_notifications_allow))
                }
            }
        }
    }
}

@Composable
private fun CampaignErrorRow(onRetry: () -> Unit) {
    Column {
        Text(
            text = stringResource(Res.string.reminders_campaign_error),
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.reminders_retry)) }
    }
}

@Composable
private fun ReminderCard(
    row: ReminderRow,
    onEnabledChange: (Boolean) -> Unit,
    onSendNow: () -> Unit,
    onDelete: () -> Unit,
) {
    val reminder = row.reminder
    var menuOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${countryLabel(row.country, reminder)} · ${reminder.language.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.reminders_more_cd))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.reminders_send_now)) },
                            onClick = {
                                menuOpen = false
                                onSendNow()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.reminders_delete)) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(platformsLabel(reminder), style = MaterialTheme.typography.bodyMedium)
                Text(scheduleSummary(reminder.schedule), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = nextWindowLabel(row.nextWindowStart, reminder.enabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val post = row.nextPost
                Text(
                    text = if (post != null) {
                        stringResource(Res.string.reminders_next_post, post.id)
                    } else {
                        stringResource(Res.string.reminders_no_posts)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (post != null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

/** Flag and name once the campaign is loaded; the bare country code until then (offline). */
private fun countryLabel(country: CampaignCountry?, reminder: ActionReminder): String =
    if (country != null) "${country.flag} ${country.countryName}" else reminder.countryCode.uppercase()

private fun platformsLabel(reminder: ActionReminder): String =
    listOfNotNull("X".takeIf { reminder.postToX }, "Facebook".takeIf { reminder.postToFacebook })
        .joinToString(" · ")

@Composable
private fun nextWindowLabel(next: Instant?, enabled: Boolean): String = when {
    !enabled -> stringResource(Res.string.reminders_paused)
    next == null -> stringResource(Res.string.reminders_no_next)
    next <= Clock.System.now() -> stringResource(Res.string.reminders_due_now)
    else -> stringResource(Res.string.reminders_next, formatMediumDateTime(next.toEpochMilliseconds()))
}

@Composable
private fun scheduleSummary(schedule: ReminderSchedule): String {
    val days = when (schedule) {
        is ReminderSchedule.Recurring ->
            if (schedule.days.size == DayOfWeek.entries.size) {
                stringResource(Res.string.reminders_every_day)
            } else {
                DayOfWeek.entries.filter { it in schedule.days }.map { shortDayName(it) }.joinToString(", ")
            }
        is ReminderSchedule.Once -> formatMediumDateForLanguage(schedule.date, Locale.current.toLanguageTag())
    }
    return "$days · ${schedule.window.start.hhMm()}–${schedule.window.end.hhMm()}"
}

@Composable
private fun shortDayName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> Res.string.reminders_day_mon
        DayOfWeek.TUESDAY -> Res.string.reminders_day_tue
        DayOfWeek.WEDNESDAY -> Res.string.reminders_day_wed
        DayOfWeek.THURSDAY -> Res.string.reminders_day_thu
        DayOfWeek.FRIDAY -> Res.string.reminders_day_fri
        DayOfWeek.SATURDAY -> Res.string.reminders_day_sat
        DayOfWeek.SUNDAY -> Res.string.reminders_day_sun
    },
)

private fun LocalTime.hhMm(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

// --- Create sheet (spec §2.2) ---

private enum class TimeField { Start, End }

/** The country's default language when it has posts in it, otherwise its first language with posts. */
private fun CampaignCountry.reminderLanguage(): String? =
    defaultLanguage.takeIf { it in postsByLanguage } ?: postsByLanguage.keys.firstOrNull()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateReminderSheet(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onSave: (ReminderDraft) -> ReminderDraftError?,
) {
    val countries = campaign.countriesWithPosts
    var draft by remember(campaign) {
        mutableStateOf(
            countries.firstOrNull()
                ?.let { ReminderDraft(countryCode = it.countryCode, language = it.reminderLanguage()) }
                ?: ReminderDraft(),
        )
    }
    var error by remember { mutableStateOf<ReminderDraftError?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<TimeField?>(null) }
    val selectedCountry = countries.firstOrNull { it.countryCode == draft.countryCode }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.reminders_create_title), style = MaterialTheme.typography.headlineSmall)

            SectionLabel(stringResource(Res.string.reminders_field_country))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                countries.forEach { country ->
                    FilterChip(
                        selected = country.countryCode == draft.countryCode,
                        onClick = {
                            draft = draft.copy(countryCode = country.countryCode, language = country.reminderLanguage())
                        },
                        label = { Text("${country.flag} ${country.countryName}") },
                    )
                }
            }

            // Only worth asking when there is a choice.
            if (selectedCountry != null && selectedCountry.postsByLanguage.size > 1) {
                SectionLabel(stringResource(Res.string.reminders_field_language))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedCountry.postsByLanguage.keys.forEach { language ->
                        FilterChip(
                            selected = language == draft.language,
                            onClick = { draft = draft.copy(language = language) },
                            label = { Text(language.uppercase()) },
                        )
                    }
                }
            }

            SectionLabel(stringResource(Res.string.reminders_field_platforms))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.postToX,
                    onClick = { draft = draft.copy(postToX = !draft.postToX) },
                    label = { Text("X") },
                )
                FilterChip(
                    selected = draft.postToFacebook,
                    onClick = { draft = draft.copy(postToFacebook = !draft.postToFacebook) },
                    label = { Text("Facebook") },
                )
            }

            SectionLabel(stringResource(Res.string.reminders_field_schedule))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.recurring,
                    onClick = { draft = draft.copy(recurring = true) },
                    label = { Text(stringResource(Res.string.reminders_schedule_weekly)) },
                )
                FilterChip(
                    selected = !draft.recurring,
                    onClick = { draft = draft.copy(recurring = false) },
                    label = { Text(stringResource(Res.string.reminders_schedule_once)) },
                )
            }
            if (draft.recurring) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in draft.days,
                            onClick = {
                                draft = draft.copy(days = if (day in draft.days) draft.days - day else draft.days + day)
                            },
                            label = { Text(shortDayName(day)) },
                        )
                    }
                }
            } else {
                OutlinedButton(onClick = { pickingDate = true }) {
                    Text(
                        draft.date?.let { formatMediumDateForLanguage(it, Locale.current.toLanguageTag()) }
                            ?: stringResource(Res.string.reminders_pick_date),
                    )
                }
            }

            SectionLabel(stringResource(Res.string.reminders_field_window))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickingTime = TimeField.Start }) {
                    Text(stringResource(Res.string.reminders_window_from, draft.start.hhMm()))
                }
                OutlinedButton(onClick = { pickingTime = TimeField.End }) {
                    Text(stringResource(Res.string.reminders_window_until, draft.end.hhMm()))
                }
            }

            error?.let {
                Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.reminders_cancel)) }
                Button(onClick = { error = onSave(draft) }) { Text(stringResource(Res.string.reminders_save)) }
            }
        }
    }

    if (pickingDate) {
        // DatePicker works in UTC midnights; the draft holds a plain calendar date.
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = draft.date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            draft = draft.copy(date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                        }
                        pickingDate = false
                    },
                ) { Text(stringResource(Res.string.reminders_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(stringResource(Res.string.reminders_cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    pickingTime?.let { field ->
        key(field) {
            val initial = if (field == TimeField.Start) draft.start else draft.end
            val timeState = rememberTimePickerState(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = { pickingTime = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val time = LocalTime(timeState.hour, timeState.minute)
                            draft = if (field == TimeField.Start) draft.copy(start = time) else draft.copy(end = time)
                            pickingTime = null
                        },
                    ) { Text(stringResource(Res.string.reminders_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pickingTime = null }) { Text(stringResource(Res.string.reminders_cancel)) }
                },
                text = { TimePicker(state = timeState) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ReminderDraftError.message(): String = stringResource(
    when (this) {
        ReminderDraftError.NoPlatform -> Res.string.reminders_error_no_platform
        ReminderDraftError.NoCountry -> Res.string.reminders_error_no_country
        ReminderDraftError.NoPosts -> Res.string.reminders_error_no_posts
        ReminderDraftError.NoWeekday -> Res.string.reminders_error_no_weekday
        ReminderDraftError.NoDate -> Res.string.reminders_error_no_date
        ReminderDraftError.DateInPast -> Res.string.reminders_error_date_past
        ReminderDraftError.WindowTooShort -> Res.string.reminders_error_window
    },
)
