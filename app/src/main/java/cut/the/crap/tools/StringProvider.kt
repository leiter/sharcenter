package cut.the.crap.tools

import android.content.Context
import androidx.annotation.StringRes

/**
 * Resolves string resources outside of Compose (repositories, ViewModels, etc.) so that
 * user-facing messages produced in the data layer are localised rather than hard-coded.
 * Kept as an interface so it can be faked in unit tests without an Android context.
 */
interface StringProvider {
    fun get(@StringRes resId: Int): String
    fun get(@StringRes resId: Int, vararg formatArgs: Any): String
}

class AndroidStringProvider constructor(
    private val context: Context
) : StringProvider {
    override fun get(resId: Int): String = context.getString(resId)
    override fun get(resId: Int, vararg formatArgs: Any): String =
        context.getString(resId, *formatArgs)
}
