package cut.the.crap.fake

import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.FileContents
import cut.the.crap.platform.PlatformUri

/**
 * In-memory [FileAccess].
 *
 * Replaces the mocked Android `Context` the ViewModel tests used to need: the seam means a file
 * is just bytes behind a handle, so the tests can state exactly what a "file" contains instead of
 * stubbing `ContentResolver`.
 */
class FakeFileAccess(
    private val files: MutableMap<PlatformUri, FileContents> = mutableMapOf(),
) : FileAccess {

    fun addTextFile(uri: String, content: String, fileName: String = "test.csv") {
        files[PlatformUri(uri)] = FileContents(
            fileName = fileName,
            mimeType = "text/plain",
            bytes = content.encodeToByteArray(),
        )
    }

    /** Everything [saveToDownloads] was asked to write, by file name, most recent last. */
    val saved = mutableListOf<Pair<String, String>>()

    /** When set, [saveToDownloads] fails with this instead of recording the write. */
    var saveFailure: Throwable? = null

    override suspend fun read(uri: PlatformUri): FileContents? = files[uri]

    override suspend fun readText(uri: PlatformUri): String? =
        files[uri]?.bytes?.decodeToString()

    override suspend fun saveToDownloads(fileName: String, text: String): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        saved += fileName to text
        return Result.success(Unit)
    }
}
