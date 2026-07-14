package cut.the.crap.platform

/**
 * An opaque handle to a file the user picked.
 *
 * Android hands back a SAF/MediaStore `content://` Uri; desktop will hand back a filesystem
 * path. Neither meaning belongs in shared code, so the handle travels as an opaque string and
 * only the platform's [FileAccess] knows how to open it. `Uri.toString()` / `Uri.parse()`
 * round-trip losslessly, so nothing is lost crossing this boundary.
 */
data class PlatformUri(val value: String) {
    override fun toString(): String = value
}

/** A file's contents plus the metadata an upload needs. */
data class FileContents(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    // ByteArray needs structural equality to keep this a well-behaved data class.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileContents) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * Reading user-picked files, without the caller knowing what a `ContentResolver` is.
 *
 * An interface rather than `expect`/`actual`: the Android implementation needs a `Context`, and
 * `expect` objects cannot carry state. Supplied by DI.
 */
interface FileAccess {
    /** Name, MIME type and bytes. Null if the file cannot be read. */
    suspend fun read(uri: PlatformUri): FileContents?

    /** The file decoded as UTF-8 text. Null if it cannot be read. */
    suspend fun readText(uri: PlatformUri): String?

    /**
     * Writes [text] as UTF-8 to [fileName] in the user's Downloads, replacing any existing file.
     *
     * The caller says *what* to save and under what name; where "Downloads" is, and whether that
     * means a MediaStore insert or an ordinary file write, is the platform's business.
     *
     * This deliberately takes a name and a string rather than an `OutputStream`. The export action
     * used to carry a `java.io.OutputStream` all the way from the screen into the ViewModel, which
     * is both untestable and un-shareable — and it forced the screen to intercept its own action,
     * open a stream, and re-dispatch it.
     */
    suspend fun saveToDownloads(fileName: String, text: String): Result<Unit>
}
