package cut.the.crap.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.IOException

/**
 * Locally hides campaign posts the user isn't interested in. This is a device-only preference, not
 * a real delete: campaign content is server-owned. Only tracks *that* a post was hidden, not when —
 * no "unhide" is offered in this pass.
 */
const val CAMPAIGN_HIDDEN_POSTS_STORE = "campaign_hidden_posts"

class CampaignHiddenPostsRepository(
    private val dataStore: DataStore<Preferences>
) {
    val hiddenKeys: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs.decodeHiddenKeys() }

    suspend fun hide(key: String) {
        dataStore.edit { prefs ->
            prefs[HIDDEN_KEYS_JSON] = json.encodeToString(prefs.decodeHiddenKeys() + key)
        }
    }

    private fun Preferences.decodeHiddenKeys(): Set<String> {
        val raw = this[HIDDEN_KEYS_JSON] ?: return emptySet()
        return runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
    }

    companion object {
        private val HIDDEN_KEYS_JSON = stringPreferencesKey("campaign_hidden_posts_json")
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * A [cut.the.crap.data.rest.campaign.CampaignPost.id] is only a variant label (e.g. "IT-1") — unique
 * within a country's language, not across the whole campaign — so hiding needs the campaign and
 * country alongside it.
 */
fun campaignPostHideKey(campaignId: String, countryCode: String, postId: String) =
    "$campaignId:$countryCode:$postId"
