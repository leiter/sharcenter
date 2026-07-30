package cut.the.crap.identity

import cut.the.crap.platform.JvmCryptoProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The recovery-phrase encoding.
 *
 * The four round-trip vectors are the published BIP-39 (Trezor) 256-bit test vectors. They are the
 * reason this file matters: they pin the encoding to the standard, so a user's written-down phrase
 * keeps working across any future change to the wordlist source or the bit-packing code.
 */
class Bip39Test {

    private val bip39 = Bip39(JvmCryptoProvider())

    @Test
    fun `wordlist is the canonical BIP-39 English list`() {
        assertEquals(2048, BIP39_ENGLISH.size)
        assertEquals(BIP39_ENGLISH.sorted(), BIP39_ENGLISH, "wordlist must stay in sorted order")
        assertEquals(2048, BIP39_ENGLISH.toSet().size, "wordlist must have no duplicates")
        // BIP-39 guarantees the first four characters identify a word uniquely.
        assertEquals(2048, BIP39_ENGLISH.map { it.take(4) }.toSet().size)
        assertTrue(BIP39_ENGLISH.all { word -> word.all { it in 'a'..'z' } })
        assertEquals("abandon", BIP39_ENGLISH.first())
        assertEquals("zoo", BIP39_ENGLISH.last())
    }

    @Test
    fun `encodes the published 256-bit test vectors`() {
        vectors.forEach { (seed, phrase) ->
            assertEquals(phrase.split(" "), bip39.encode(seed))
        }
    }

    @Test
    fun `decodes the published 256-bit test vectors`() {
        vectors.forEach { (seed, phrase) ->
            val result = bip39.decode(phrase)
            assertIs<MnemonicResult.Valid>(result)
            assertContentEquals(seed, result.seed)
        }
    }

    @Test
    fun `round-trips arbitrary seeds`() {
        val crypto = JvmCryptoProvider()
        repeat(200) {
            val seed = crypto.randomBytes(32)
            val result = bip39.decode(bip39.encode(seed).joinToString(" "))
            assertIs<MnemonicResult.Valid>(result)
            assertContentEquals(seed, result.seed)
        }
    }

    @Test
    fun `normalises case and whitespace`() {
        val seed = ByteArray(32)
        val messy = "  ABANDON abandon\tabandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon\nabandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon abandon   Art  "
        assertContentEquals(seed, assertIs<MnemonicResult.Valid>(bip39.decode(messy)).seed)
    }

    @Test
    fun `reports the wrong number of words`() {
        assertEquals(MnemonicResult.WrongLength(3), bip39.decode("abandon abandon art"))
        assertEquals(MnemonicResult.WrongLength(0), bip39.decode("   "))
    }

    @Test
    fun `reports a word that is not in the list`() {
        val phrase = ("abandon ".repeat(23) + "banana42").trim()
        assertEquals(MnemonicResult.UnknownWord("banana42"), bip39.decode(phrase))
    }

    @Test
    fun `rejects a phrase whose checksum does not match`() {
        // All 24 words valid, but the last is the wrong one for this entropy.
        val phrase = ("abandon ".repeat(23) + "zoo").trim()
        assertEquals(MnemonicResult.ChecksumMismatch, bip39.decode(phrase))
    }

    @Test
    fun `rejects a phrase with two words transposed`() {
        // The checksum's real job: catching a phrase copied back in the wrong order.
        val words = bip39.encode(JvmCryptoProvider().randomBytes(32)).toMutableList()
        val swapped = words[0]
        words[0] = words[1]
        words[1] = swapped
        assertIs<MnemonicResult.ChecksumMismatch>(bip39.decode(words.joinToString(" ")))
    }

    private companion object {
        /**
         * BIP-39 256-bit test vectors: entropy paired with its phrase, taken from the reference
         * (Trezor) vector set.
         */
        val vectors: List<Pair<ByteArray, String>> = listOf(
            ByteArray(32) { 0x00 } to
                "abandon ".repeat(23) + "art",
            ByteArray(32) { 0x7f } to
                "legal winner thank year wave sausage worth useful legal winner thank year " +
                "wave sausage worth useful legal winner thank year wave sausage worth title",
            ByteArray(32) { 0x80.toByte() } to
                "letter advice cage absurd amount doctor acoustic avoid letter advice cage " +
                "absurd amount doctor acoustic avoid letter advice cage absurd amount doctor " +
                "acoustic bless",
            ByteArray(32) { 0xff.toByte() } to
                "zoo ".repeat(23) + "vote",
        ).map { (seed, phrase) -> seed to phrase.trim() }
    }
}

private fun assertContentEquals(expected: ByteArray, actual: ByteArray) =
    assertTrue(
        expected.contentEquals(actual),
        "expected ${expected.toHex()} but was ${actual.toHex()}",
    )

private fun ByteArray.toHex(): String = joinToString("") { byte ->
    val value = byte.toInt() and 0xFF
    "0123456789abcdef"[value shr 4].toString() + "0123456789abcdef"[value and 0x0F]
}
