package cut.the.crap.platform

import cut.the.crap.data.preferences.appDataDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * The identity seed as a plain file in the application data directory, readable only by its owner.
 *
 * Weaker than the Android implementation, which wraps the seed in a hardware-backed key: here the
 * seed is protected by filesystem permissions alone, so anything running as this user can read it.
 * That is the honest desktop baseline — `doc/IDENTITY_SPEC.md` §3.1 accepts it for v1 rather than
 * pulling in an OS-keyring dependency, and it is stated here so nobody assumes otherwise.
 *
 * @param directory overridable so tests do not touch the real user profile.
 */
class DesktopIdentityKeyStore(
    directory: String = appDataDirectory(),
) : IdentityKeyStore {

    private val file = File(directory, FILE_NAME)

    override suspend fun loadSeed(): ByteArray? = withContext(Dispatchers.IO) {
        if (file.exists()) file.readBytes() else null
    }

    override suspend fun storeSeed(seed: ByteArray): Unit = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        // Create the file empty and restrict it *before* the seed goes in, so it is never briefly
        // world-readable.
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.delete()
        temp.createNewFile()
        restrictToOwner(temp)
        temp.writeBytes(seed)
        check(temp.renameTo(file)) { "Could not move the identity seed into place." }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        file.delete()
        Unit
    }

    /**
     * `chmod 600`. POSIX-only: on Windows the call throws and the file keeps the directory's
     * inherited ACL, which is per-user under `%APPDATA%` anyway.
     */
    private fun restrictToOwner(target: File) {
        runCatching {
            Files.setPosixFilePermissions(
                target.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }.onFailure { Log.d(TAG, "POSIX permissions unavailable: ${it.message}") }
    }

    private companion object {
        const val TAG = "DesktopIdentityKeyStore"
        const val FILE_NAME = "identity.seed"
    }
}
