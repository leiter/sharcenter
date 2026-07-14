package cut.the.crap.tools

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A random UUID as a string.
 *
 * `java.util.UUID` is JVM-only; Kotlin's own `Uuid` is multiplatform, so no seam is needed — just
 * an opt-in, since the API is still experimental in Kotlin 2.1.
 */
@OptIn(ExperimentalUuidApi::class)
fun randomUuid(): String = Uuid.random().toString()
