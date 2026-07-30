package cut.the.crap.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Base64UrlTest {

    @Test
    fun `encodes without padding and with the url alphabet`() {
        // 0xFB 0xFF exercises both characters that differ from standard base64 ('+' and '/').
        assertEquals("-_8", byteArrayOf(0xFB.toByte(), 0xFF.toByte()).encodeBase64Url())
        assertEquals("", ByteArray(0).encodeBase64Url())
        assertTrue(ByteArray(32).encodeBase64Url().none { it == '=' })
    }

    @Test
    fun `round-trips every remainder length`() {
        (0..8).forEach { size ->
            val bytes = ByteArray(size) { (it * 37).toByte() }
            assertTrue(bytes.contentEquals(bytes.encodeBase64Url().decodeBase64Url()))
        }
    }

    @Test
    fun `accepts padded input too`() {
        assertTrue(byteArrayOf(1, 2).contentEquals("AQI=".decodeBase64Url()))
    }

    @Test
    fun `rejects a length that cannot decode to whole bytes`() {
        assertFailsWith<IllegalArgumentException> { "A".decodeBase64Url() }
    }
}
