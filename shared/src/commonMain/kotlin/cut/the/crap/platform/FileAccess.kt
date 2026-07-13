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
}
