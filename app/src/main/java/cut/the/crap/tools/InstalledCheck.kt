package cut.the.crap.tools

import android.content.Context
import android.content.pm.PackageManager


const val TWITTER_PACKAGE = "com.twitter.android"
const val INSTAGRAM_PACKAGE = "com.instagram.android"
const val FACEBOOK_PACKAGE = "com.facebook.katana"

object InstalledCheck {

    fun isTwitterInstalled(context: Context): Boolean {
        return isInstalled(context, TWITTER_PACKAGE)
    }

    fun isInstagramInstalled(context: Context): Boolean {
        return isInstalled(context, INSTAGRAM_PACKAGE)
    }

    fun isFacebookInstalled(context: Context): Boolean {
        return isInstalled(context, FACEBOOK_PACKAGE)
    }

    private fun isInstalled(context: Context, packageName: String): Boolean{
        val packageManager = context.packageManager
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

