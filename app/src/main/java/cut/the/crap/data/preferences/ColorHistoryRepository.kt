package cut.the.crap.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Persists the colours the user has recently picked in the [cut.the.crap.ui.components.ColorPicker],
 * most-recently-picked first. Backs the picker's history preview strip: re-picking a colour moves it
 * to the front so it shows up first the next time the picker opens.
 *
 * Colours are stored as six-digit uppercase RRGGBB strings (matching `Color.toHexString()`), so this
 * layer stays free of Compose/Android colour types for the planned KMP migration.
 */
class ColorHistoryRepository constructor(
    private val context: Context
) {
    @Serializable
    private data class StoredColor(val hex: String, val pickedAt: Long)

    /** Recently picked colours as RRGGBB hex strings, most recent first. */
    val recentColors: Flow<List<String>> = context.colorHistoryStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs.decodeStoredColors().map { it.hex } }

    /**
     * Records [hex] as the most recently picked colour. Any existing entry for the same colour is
     * removed first, so the colour moves to the front rather than duplicating, and the list is
     * trimmed to [MAX_STORED].
     */
    suspend fun recordColor(hex: String) {
        val normalized = normalizeHex(hex) ?: return
        context.colorHistoryStore.edit { prefs ->
            val current = prefs.decodeStoredColors()
            val updated = buildList {
                add(StoredColor(normalized, System.currentTimeMillis()))
                addAll(current.filter { !it.hex.equals(normalized, ignoreCase = true) })
            }.take(MAX_STORED)
            prefs[RECENT_COLORS_JSON] = json.encodeToString(updated)
        }
    }

    private fun Preferences.decodeStoredColors(): List<StoredColor> {
        val raw = this[RECENT_COLORS_JSON] ?: return emptyList()
        return runCatching { json.decodeFromString<List<StoredColor>>(raw) }.getOrDefault(emptyList())
    }

    /** Uppercase RRGGBB with no `#`, or null if the input is not a six-digit hex colour. */
    private fun normalizeHex(hex: String): String? {
        val cleaned = hex.trim().removePrefix("#").uppercase()
        if (cleaned.length != 6) return null
        if (cleaned.any { it !in "0123456789ABCDEF" }) return null
        return cleaned
    }

    companion object {
        /** How many colours to keep. The picker previews a subset and expands to show the rest. */
        const val MAX_STORED = 30
        private val RECENT_COLORS_JSON = stringPreferencesKey("recent_colors_json")
        private val json = Json { ignoreUnknownKeys = true }
    }
}

private val Context.colorHistoryStore: DataStore<Preferences> by preferencesDataStore(name = "color_history")
