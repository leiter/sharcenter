package cut.the.crap.reminder

import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ReminderPost
import cut.the.crap.data.preferences.campaignPostHideKey

/**
 * The reminder's posts the user has not hidden, in snapshot order — what rotation walks through.
 * [hiddenPostKeys] are in `campaignPostHideKey` format.
 */
fun ActionReminder.visiblePosts(hiddenPostKeys: Set<String>): List<ReminderPost> =
    posts.filter { campaignPostHideKey(campaignId, countryCode, it.id) !in hiddenPostKeys }

/** The post the reminder shows next, or null when every post is hidden. */
fun ActionReminder.nextPost(hiddenPostKeys: Set<String>): ReminderPost? =
    visiblePosts(hiddenPostKeys).let { if (it.isEmpty()) null else it[nextPostIndex.mod(it.size)] }
