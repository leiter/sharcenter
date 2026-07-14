package cut.the.crap.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.nio.file.Files

/**
 * Resolves [PlatformUri]s as ordinary filesystem paths.
 *
 * Accepts both a bare path and a `file:` URI, because an AWT `FileDialog` yields the former
 * while anything round-tripped through a URI yields the latter.
 */
class DesktopFileAccess : FileAccess {

    override suspend fun read(uri: PlatformUri): FileContents? = withContext(Dispatchers.IO) {
        val file = uri.toFile() ?: return@withContext null
        try {
            FileContents(
                fileName = file.name,
                mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream",
                bytes = file.readBytes(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: $uri", e)
            null
        }
    }

    override suspend fun readText(uri: PlatformUri): String? = withContext(Dispatchers.IO) {
        try {
            uri.toFile()?.readText()
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
            // No MediaStore here — "Downloads" is just a directory, and it may not exist.
            val downloads = File(System.getProperty("user.home"), "Downloads")
            if (!downloads.exists()) downloads.mkdirs()
            File(downloads, fileName).writeText(text)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving $fileName", e)
            Result.failure(e)
        }
    }

    private fun PlatformUri.toFile(): File? = try {
        if (value.startsWith("file:")) File(URI(value)) else File(value)
    } catch (e: Exception) {
        Log.e(TAG, "Not a usable path: $value", e)
        null
    }

    private companion object {
        const val TAG = "DesktopFileAccess"
    }
}
