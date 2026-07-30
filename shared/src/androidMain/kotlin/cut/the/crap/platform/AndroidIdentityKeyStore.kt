package cut.the.crap.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The identity seed, encrypted under a hardware-backed AES key held in the Android Keystore and
 * written to a file in `filesDir`.
 *
 * The wrapping key is non-exportable and never leaves the Keystore, so the file on disk is useless
 * on its own — while the *seed* itself stays portable via the recovery phrase, which is the
 * trade-off `doc/IDENTITY_SPEC.md` D3 chose deliberately (a non-exportable Ed25519 key would mean
 * a dead phone destroys campaign ownership).
 *
 * `androidx.security:security-crypto` is deliberately not used: it is deprecated, and this is the
 * same construction with fewer moving parts.
 *
 * The file lives in `filesDir`, not in the app database, so `DatabaseBackupManager`'s daily copy
 * into Downloads never contains it (§6.2).
 */
class AndroidIdentityKeyStore(context: Context) : IdentityKeyStore {

    private val file = File(context.filesDir, FILE_NAME)

    override suspend fun loadSeed(): ByteArray? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            val stored = file.readBytes()
            // Layout: [12-byte GCM IV][ciphertext+tag].
            if (stored.size <= IV_SIZE) return@withContext null
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    wrappingKey(),
                    GCMParameterSpec(TAG_BITS, stored, 0, IV_SIZE),
                )
            }
            cipher.doFinal(stored, IV_SIZE, stored.size - IV_SIZE)
        } catch (e: Exception) {
            // A failure here means the wrapping key is gone (app data cleared, device restored to
            // new hardware) or the file is truncated. Either way the seed is unrecoverable from
            // this device and the user must restore from their phrase — report "no identity"
            // rather than crashing.
            Log.w(TAG, "Stored identity seed could not be read; treating as absent.", e)
            null
        }
    }

    override suspend fun storeSeed(seed: ByteArray): Unit = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, wrappingKey())
        }
        val encrypted = cipher.doFinal(seed)
        // Write-then-rename so an interrupted write cannot leave a half-file that reads as a
        // corrupt identity.
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.writeBytes(cipher.iv + encrypted)
        check(temp.renameTo(file)) { "Could not move the identity seed into place." }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        file.delete()
        // Drop the wrapping key too, so nothing usable survives a reset.
        runCatching { androidKeyStore().deleteEntry(KEY_ALIAS) }
            .onFailure { Log.w(TAG, "Could not delete the identity wrapping key.", it) }
        Unit
    }

    private fun androidKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** The AES key wrapping the seed, generated on first use and never exported. */
    private fun wrappingKey(): SecretKey {
        val keyStore = androidKeyStore()
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    // No setUserAuthenticationRequired: requests are signed in the background,
                    // long after any unlock, so a per-use auth requirement would break them.
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val TAG = "AndroidIdentityKeyStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "cut.the.crap.identity.seed"
        const val FILE_NAME = "identity.seed"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
