package cut.the.crap.tools

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Unpadded base64url — the encoding `doc/IDENTITY_SPEC.md` §4 uses for public keys, signatures and
 * digests, chosen because the values travel in an HTTP header where `+`, `/` and `=` are awkward.
 *
 * Padding is stripped and restored by hand rather than via `Base64.PaddingOption`, which arrived in
 * a later Kotlin than this project's floor; the four lines here work on any version.
 */
@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.encodeBase64Url(): String = Base64.UrlSafe.encode(this).trimEnd('=')

/**
 * Decodes unpadded (or padded) base64url.
 *
 * @throws IllegalArgumentException if [this] is not valid base64url.
 */
@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64Url(): ByteArray {
    val padded = when (length % 4) {
        0 -> this
        2 -> "$this=="
        3 -> "$this="
        // A base64 group is never 1 character; that length cannot decode to whole bytes.
        else -> throw IllegalArgumentException("Not valid base64url: length %4 == ${length % 4}")
    }
    return Base64.UrlSafe.decode(padded)
}
