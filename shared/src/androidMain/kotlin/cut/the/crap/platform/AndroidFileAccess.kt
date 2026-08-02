package cut.the.crap.platform

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

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

    override suspend fun saveToDownloads(
        fileName: String,
        text: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stream = openDownloadsStream(fileName)
                ?: return@withContext Result.failure(IOException("Could not open $fileName for writing"))
            stream.use { it.write(text.toByteArray()) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving $fileName", e)
            Result.failure(e)
        }
    }

    /** MediaStore on Q+, a plain file below it — the split the old `provideOutputStream` made. */
    private fun openDownloadsStream(fileName: String): OutputStream? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?.let { resolver.openOutputStream(it) }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            FileOutputStream(File(downloadsDir, fileName))
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
