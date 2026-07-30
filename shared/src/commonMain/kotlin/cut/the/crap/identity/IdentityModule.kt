package cut.the.crap.identity

import cut.the.crap.data.rest.identity.IdentityRepository
import cut.the.crap.data.rest.identity.IdentityRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * The identity layer: key material, and the two calls that register it with the server.
 *
 * Loaded per platform alongside the module that binds [cut.the.crap.platform.CryptoProvider] and
 * [cut.the.crap.platform.IdentityKeyStore]. **iOS deliberately does not load it yet** — it has no
 * implementation of those two seams (`doc/IDENTITY_SPEC.md` §3.2), and loading this module there
 * would turn a missing binding into a startup crash. The `CtcSignature` plugin resolves
 * [RequestSigner] optionally for exactly that reason, so an iOS build simply sends unsigned
 * requests until its implementations land.
 */
val identityModule = module {
    singleOf(::IdentityManager)
    single<RequestSigner> { get<IdentityManager>() }
    singleOf(::IdentityRepositoryImpl) bind IdentityRepository::class
}
