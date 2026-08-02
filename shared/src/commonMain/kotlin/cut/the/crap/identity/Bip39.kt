package cut.the.crap.identity

import cut.the.crap.platform.CryptoProvider
import cut.the.crap.platform.ED25519_SEED_SIZE

/**
 * BIP-39 encoding of the 32-byte identity seed as a 24-word recovery phrase.
 *
 * Only the 256-bit case is implemented, because that is the only entropy size the identity uses
 * (`doc/IDENTITY_SPEC.md` §6.1). 256 bits of entropy plus an 8-bit checksum is 264 bits, which is
 * exactly 24 groups of 11 — one wordlist index each.
 *
 * This is the *encoding* half of BIP-39 only. The passphrase/PBKDF2 seed-stretching half is
 * deliberately absent: the phrase here reproduces the Ed25519 seed itself, and is not used to
 * derive a hierarchy of wallet keys.
 */
class Bip39(private val crypto: CryptoProvider) {

    /**
     * Encodes a 32-byte [seed] as its 24-word phrase.
     *
     * @throws IllegalArgumentException if [seed] is not 32 bytes.
     */
    fun encode(seed: ByteArray): List<String> {
        require(seed.size == ED25519_SEED_SIZE) {
            "Seed must be $ED25519_SEED_SIZE bytes, was ${seed.size}"
        }

        // Checksum is the first CS = ENT/32 = 8 bits of SHA-256(entropy) — i.e. its first byte.
        val bits = seed + crypto.sha256(seed)[0]

        return List(WORD_COUNT) { word ->
            var index = 0
            repeat(BITS_PER_WORD) { offset ->
                index = (index shl 1) or bits.bitAt(word * BITS_PER_WORD + offset)
            }
            BIP39_ENGLISH[index]
        }
    }

    /**
     * Decodes a recovery phrase back to its seed.
     *
     * Accepts any capitalisation and any run of whitespace between words; the English wordlist is
     * pure lower-case ASCII, so normalising case and whitespace is sufficient (no Unicode NFKD
     * step is needed as it would be for some other BIP-39 languages).
     *
     * Returns a [MnemonicResult] rather than throwing, so the recovery UI can say *which* of the
     * three ways a phrase can be wrong actually happened.
     */
    fun decode(phrase: String): MnemonicResult {
        val words = phrase.trim().lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.size != WORD_COUNT) return MnemonicResult.WrongLength(words.size)

        val bits = ByteArray(ED25519_SEED_SIZE + 1)
        var position = 0
        for (word in words) {
            val index = BIP39_INDEX[word] ?: return MnemonicResult.UnknownWord(word)
            for (offset in BITS_PER_WORD - 1 downTo 0) {
                bits.setBitAt(position++, (index shr offset) and 1)
            }
        }

        val seed = bits.copyOf(ED25519_SEED_SIZE)
        // Recompute rather than trust: the checksum is the only thing standing between a
        // mistyped word and a silently wrong identity.
        if (bits[ED25519_SEED_SIZE] != crypto.sha256(seed)[0]) return MnemonicResult.ChecksumMismatch

        return MnemonicResult.Valid(seed)
    }

    private companion object {
        const val WORD_COUNT = 24
        const val BITS_PER_WORD = 11
        val WHITESPACE = Regex("\\s+")

        /** Reverse of [BIP39_ENGLISH]; built once, since decoding touches it 24 times per call. */
        val BIP39_INDEX: Map<String, Int> =
            BIP39_ENGLISH.withIndex().associate { (index, word) -> word to index }
    }
}

/** Bit [position] of the array, counting from the most significant bit of byte 0. */
private fun ByteArray.bitAt(position: Int): Int =
    (this[position / 8].toInt() shr (7 - position % 8)) and 1

private fun ByteArray.setBitAt(position: Int, bit: Int) {
    if (bit == 0) return
    val index = position / 8
    this[index] = (this[index].toInt() or (1 shl (7 - position % 8))).toByte()
}

/** The outcome of decoding a recovery phrase. */
sealed interface MnemonicResult {

    /** The phrase was well-formed and its checksum matched. */
    data class Valid(val seed: ByteArray) : MnemonicResult {
        // ByteArray uses identity equals; a data class holding one needs both written out.
        override fun equals(other: Any?): Boolean =
            this === other || (other is Valid && seed.contentEquals(other.seed))

        override fun hashCode(): Int = seed.contentHashCode()
    }

    /** The phrase did not have 24 words. */
    data class WrongLength(val actual: Int) : MnemonicResult

    /** [word] is not in the BIP-39 English wordlist — typically a typo. */
    data class UnknownWord(val word: String) : MnemonicResult

    /** Every word is valid but the checksum failed: a word is misspelled, swapped or reordered. */
    data object ChecksumMismatch : MnemonicResult
}
