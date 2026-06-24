package cut.the.crap.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val BASE_PREFS = "BASE_PREFS"
private const val KEY_DEFAULT_DAYS_TO_LOAD = "KEY_DEFAULT_DAYS_TO_LOAD"
const val DEFAULT_DAYS_TO_LOAD = 7

object PrefsFile {

    fun getDefaultDays(context: Context): Int {
        return prefs(context).getInt(KEY_DEFAULT_DAYS_TO_LOAD, DEFAULT_DAYS_TO_LOAD)
    }

    fun setDefaultDays(days: Int, context: Context) {
        prefs(context).edit { putInt(KEY_DEFAULT_DAYS_TO_LOAD, days) }
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(BASE_PREFS, Context.MODE_PRIVATE)
    }

}