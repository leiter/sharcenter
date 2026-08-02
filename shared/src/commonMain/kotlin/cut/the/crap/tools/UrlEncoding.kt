package cut.the.crap.tools

/**
 * Percent-encodes [this] for use inside a URL query value.
 *
 * A drop-in replacement for `android.net.Uri.encode`, and deliberately character-for-character
 * identical to it: everything outside `[A-Za-z0-9]` and `_-!.~'()*` is UTF-8 encoded as
 * uppercase `%XX` triplets. That set is Android's, not RFC 3986's — matching it exactly means
 * the share URLs this app has always produced keep working byte for byte, so moving the intent
 * builders off Android cannot quietly change what gets posted to X or Facebook.
 *
 * Space becomes `%20` (not `+`), which is what `Uri.encode` does and what both services accept.
 */
fun String.urlEncode(): String {
    val out = StringBuilder(length)
    for (byte in encodeToByteArray()) {
        val char = byte.toInt().toChar()
        if (char.isSafe()) {
            out.append(char)
        } else {
            // A negative byte is a UTF-8 continuation/lead byte; mask back to 0..255.
            val value = byte.toInt() and 0xFF
            out.append('%')
            out.append(HEX[value shr 4])
            out.append(HEX[value and 0x0F])
        }
    }
    return out.toString()
}

private const val HEX = "0123456789ABCDEF"

private fun Char.isSafe(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in UNRESERVED

private const val UNRESERVED = "_-!.~'()*"
