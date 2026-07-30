package cut.the.crap.fake

import cut.the.crap.platform.IdentityKeyStore

/** In-memory [IdentityKeyStore]. [stores] counts writes, so tests can assert what was persisted. */
class FakeIdentityKeyStore(private var seed: ByteArray? = null) : IdentityKeyStore {

    var stores: Int = 0
        private set

    var clears: Int = 0
        private set

    override suspend fun loadSeed(): ByteArray? = seed

    override suspend fun storeSeed(seed: ByteArray) {
        this.seed = seed
        stores++
    }

    override suspend fun clear() {
        seed = null
        clears++
    }
}
