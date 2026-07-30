package cut.the.crap.platform

/**
 * Secure storage for the identity private seed.
 *
 * Deliberately **not** the app database. `DatabaseBackupManager` copies the database into the
 * Downloads folder on a schedule, so a seed held in SQLDelight would be written into a
 * world-readable location every day (`doc/IDENTITY_SPEC.md` §6.2). It is likewise excluded from
 * Import/Export.
 *
 * Named `IdentityKeyStore`, not `KeyStore`, so it never reads as `java.security.KeyStore` on the
 * JVM targets.
 *
 * Implementations store an opaque blob; they neither interpret the seed nor derive anything from
 * it. Loss of the stored seed is recoverable only from the user's recovery phrase — see
 * [cut.the.crap.identity.Bip39].
 */
interface IdentityKeyStore {

    /** The stored 32-byte seed, or null when this install has no identity yet. */
    suspend fun loadSeed(): ByteArray?

    /** Persists [seed], replacing any seed already stored. */
    suspend fun storeSeed(seed: ByteArray)

    /** Erases the stored seed. Backs the "reset identity" action in Settings. */
    suspend fun clear()
}
