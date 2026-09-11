@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.ui.content.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ActionReminderRepository
import cut.the.crap.data.domain.ReminderPost
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.data.domain.TimeWindow
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignCountry
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.data.rest.campaign.CampaignRepositoryImpl
import cut.the.crap.platform.ReminderNotifier
import cut.the.crap.reminder.ReminderDispatcher
import cut.the.crap.reminder.ReminderScheduleSync
import cut.the.crap.reminder.nextPost
import cut.the.crap.reminder.nextWindowStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * One row of the reminder list.
 *
 * @property country Null until the campaign has loaded, or when the campaign no longer has it.
 * @property nextWindowStart Null when the reminder never fires again; in the past while it is due.
 * @property nextPost Null when every post of the reminder is hidden or gone.
 */
data class ReminderRow(
    val reminder: ActionReminder,
    val country: CampaignCountry?,
    val nextWindowStart: Instant?,
    val nextPost: ReminderPost?,
)

data class ActionRemindersUiState(
    val rows: List<ReminderRow> = emptyList(),
    /** The campaign new reminders are made from; the list itself works without it (offline). */
    val campaign: Campaign? = null,
    val campaignLoading: Boolean = true,
    val campaignError: AppError? = null,
    /** False while notifications are blocked — the screen shows a banner then. */
    val canNotify: Boolean = true,
)

sealed interface ActionRemindersEvent {
    /** Offer undo for [reminder]. */
    data class Deleted(val reminder: ActionReminder) : ActionRemindersEvent
    data object Sent : ActionRemindersEvent
    /** "Send now" found nothing to show: every post of the reminder is hidden. */
    data object NothingToSend : ActionRemindersEvent
    /** Notifications are blocked and something needs them: ask for the permission. */
    data object RequestNotificationPermission : ActionRemindersEvent
}

/**
 * Backs the reminder screen (doc/ACTION_REMINDER_SPEC.md §2.1, §6). Reminders are made from the
 * bundled campaign only, for now (spec R9).
 *
 * @param hiddenPostKeys The user's hidden campaign posts, so each row shows the post that will
 *   really come next. A flow rather than the DataStore-backed repository, for testability.
 */
class ActionRemindersViewModel(
    private val repository: ActionReminderRepository,
    private val campaignRepository: CampaignRepository,
    private val dispatcher: ReminderDispatcher,
    private val scheduleSync: ReminderScheduleSync,
    private val notifier: ReminderNotifier,
    hiddenPostKeys: Flow<Set<String>>,
    private val clock: () -> Instant = { Clock.System.now() },
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {

    private data class CampaignLoad(
        val campaign: Campaign? = null,
        val loading: Boolean = true,
        val error: AppError? = null,
    )

    private val campaignLoad = MutableStateFlow(CampaignLoad())
    private val canNotify = MutableStateFlow(notifier.canNotify)

    val state: StateFlow<ActionRemindersUiState> = combine(
        repository.observeAll(),
        hiddenPostKeys,
        campaignLoad,
        canNotify,
    ) { reminders, hidden, load, allowed ->
        val now = clock()
        val tz = timeZone()
        ActionRemindersUiState(
            rows = reminders.map { reminder ->
                ReminderRow(
                    reminder = reminder,
                    country = load.campaign?.countries?.firstOrNull { it.countryCode == reminder.countryCode },
                    nextWindowStart = reminder.nextWindowStart(now, tz),
                    nextPost = reminder.nextPost(hidden),
                )
            },
            campaign = load.campaign,
            campaignLoading = load.loading,
            campaignError = load.error,
            canNotify = allowed,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ActionRemindersUiState(canNotify = notifier.canNotify))

    private val _events = MutableSharedFlow<ActionRemindersEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ActionRemindersEvent> = _events.asSharedFlow()

    init {
        loadCampaign()
    }

    fun loadCampaign() {
        viewModelScope.launch {
            campaignLoad.update { it.copy(loading = true, error = null) }
            when (val result = campaignRepository.get(CampaignRepositoryImpl.BUNDLED_CAMPAIGN_ID)) {
                is Result.Success -> {
                    campaignLoad.value = CampaignLoad(campaign = result.data, loading = false)
                    refreshSnapshots(result.data)
                }
                // Keep a campaign already on screen: a failed reload should not empty the sheet.
                is Result.Error -> campaignLoad.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    /**
     * Saves [draft] as a new reminder, or returns why it cannot be saved. Asks for the notification
     * permission afterwards when notifications are blocked — a reminder nobody can see is the one
     * failure the user would not notice.
     */
    fun create(draft: ReminderDraft): ReminderDraftError? {
        val campaign = campaignLoad.value.campaign
        val now = clock()
        draft.validate(campaign, now.toLocalDateTime(timeZone()).date)?.let { return it }
        // validate() guarantees all of these.
        val countryCode = draft.countryCode!!
        val language = draft.language!!
        val window = TimeWindow(draft.start, draft.end)
        val reminder = ActionReminder(
            campaignId = CampaignRepositoryImpl.BUNDLED_CAMPAIGN_ID,
            countryCode = countryCode,
            language = language,
            postToX = draft.postToX,
            postToFacebook = draft.postToFacebook,
            schedule = if (draft.recurring) {
                ReminderSchedule.Recurring(draft.days, window)
            } else {
                ReminderSchedule.Once(draft.date!!, window)
            },
            posts = campaign!!.reminderPostsFor(countryCode, language),
            campaignVersion = campaign.version,
            createdAt = now.toEpochMilliseconds(),
            modifiedAt = now.toEpochMilliseconds(),
        )
        viewModelScope.launch {
            repository.insert(reminder)
            scheduleSync.sync()
            if (!notifier.canNotify) _events.emit(ActionRemindersEvent.RequestNotificationPermission)
        }
        return null
    }

    fun setEnabled(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
            if (!enabled) notifier.cancel(id)
            scheduleSync.sync()
        }
    }

    fun delete(reminder: ActionReminder) {
        viewModelScope.launch {
            repository.delete(reminder.id)
            notifier.cancel(reminder.id)
            scheduleSync.sync()
            _events.emit(ActionRemindersEvent.Deleted(reminder))
        }
    }

    /** Puts a deleted reminder back — as a new row, with its schedule and rotation intact. */
    fun undoDelete(reminder: ActionReminder) {
        viewModelScope.launch {
            repository.insert(reminder)
            scheduleSync.sync()
        }
    }

    fun sendNow(id: Int) {
        viewModelScope.launch {
            if (!notifier.canNotify) {
                _events.emit(ActionRemindersEvent.RequestNotificationPermission)
                return@launch
            }
            val sent = dispatcher.sendNow(id, clock())
            _events.emit(if (sent) ActionRemindersEvent.Sent else ActionRemindersEvent.NothingToSend)
        }
    }

    /** Re-reads whether notifications can be shown: after the permission dialog, and on resume. */
    fun refreshNotificationPermission() {
        canNotify.value = notifier.canNotify
    }

    fun openNotificationSettings() = notifier.openSettings()

    /**
     * Spec §3.2: reminders made from an older campaign version get the current posts. A reminder
     * whose country or language no longer has any is disabled rather than deleted, so the user can
     * see what happened to it.
     */
    private suspend fun refreshSnapshots(campaign: Campaign) {
        val outdated = repository.observeAll().first().filter {
            it.campaignId == CampaignRepositoryImpl.BUNDLED_CAMPAIGN_ID && it.campaignVersion < campaign.version
        }
        if (outdated.isEmpty()) return
        outdated.forEach { reminder ->
            val posts = campaign.reminderPostsFor(reminder.countryCode, reminder.language)
            repository.update(
                reminder.copy(
                    posts = posts,
                    campaignVersion = campaign.version,
                    nextPostIndex = if (posts.isEmpty()) 0 else reminder.nextPostIndex.mod(posts.size),
                    enabled = reminder.enabled && posts.isNotEmpty(),
                ),
            )
        }
        scheduleSync.sync()
    }
}
