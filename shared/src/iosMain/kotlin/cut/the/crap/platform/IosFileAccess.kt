package cut.the.crap.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Reads user-picked files and writes exports via okio's native [FileSystem], which keeps this off
 * cinterop. A [PlatformUri] is either a `file://` URL (from the document picker) or a bare path;
 * both resolve to a filesystem path.
 *
 * ⚠️ When the real document picker lands (it currently yields nothing on iOS v1), files it returns
 * are *security-scoped* — the caller must `startAccessingSecurityScopedResource` around the read.
 * That wiring belongs with the picker; this reads plain paths for now.
 */
class IosFileAccess : FileAccess {

    override suspend fun read(uri: PlatformUri): FileContents? = withContext(Dispatchers.Default) {
        val path = uri.toOkioPath() ?: return@withContext null
        try {
            val bytes = FileSystem.SYSTEM.read(path) { readByteArray() }
            FileContents(path.name, "application/octet-stream", bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: $uri", e)
            null
        }
    }

    override suspend fun readText(uri: PlatformUri): String? = withContext(Dispatchers.Default) {
        val path = uri.toOkioPath() ?: return@withContext null
        try {
            FileSystem.SYSTEM.read(path) { readUtf8() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error reading text: $uri", e)
            null
        }
    }

    override suspend fun saveToDownloads(
        fileName: String,
        text: String,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // iOS has no Downloads folder; write into the app's Documents directory (visible in Files).
            val documents = NSSearchPathForDirectoriesInDomains(
                directory = NSDocumentDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true,
            ).first() as String
            FileSystem.SYSTEM.write("$documents/$fileName".toPath()) { writeUtf8(text) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving $fileName", e)
            Result.failure(e)
        }
    }

    private fun PlatformUri.toOkioPath(): Path? {
        val raw = if (value.startsWith("file://")) NSURL(string = value)?.path else value
        return raw?.toPath()
    }

    private companion object {
        const val TAG = "IosFileAccess"
    }
}
