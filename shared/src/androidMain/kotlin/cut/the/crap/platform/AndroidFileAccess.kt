package cut.the.crap.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolves [PlatformUri]s through the SAF/MediaStore `ContentResolver`. */
class AndroidFileAccess(private val context: Context) : FileAccess {

    private val resolver get() = context.contentResolver

    override suspend fun read(uri: PlatformUri): FileContents? = withContext(Dispatchers.IO) {
        val androidUri = uri.toAndroidUri()
        try {
            val bytes = resolver.openInputStream(androidUri)?.use { it.readBytes() }
                ?: return@withContext null
            FileContents(
                fileName = displayName(androidUri) ?: "unknown_file",
                mimeType = resolver.getType(androidUri) ?: "application/octet-stream",
                bytes = bytes,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: $uri", e)
            null
        }
    }

    override suspend fun readText(uri: PlatformUri): String? = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(uri.toAndroidUri())
                ?.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading text: $uri", e)
            null
        }
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }

    private companion object {
        const val TAG = "AndroidFileAccess"
    }
}

/** Boundary conversions. `Uri.toString()` / `Uri.parse()` round-trip losslessly. */
fun Uri.toPlatformUri(): PlatformUri = PlatformUri(toString())

fun PlatformUri.toAndroidUri(): Uri = Uri.parse(value)
