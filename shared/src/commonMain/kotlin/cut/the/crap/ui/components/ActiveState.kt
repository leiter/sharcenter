package cut.the.crap.ui.components

/**
 * Tri-state (plus disabled) filter chip state, cycling Default → Include → Exclude → Default.
 *
 * Pulled out of `MyChip.kt` so it can live in `commonMain`: it is a pure enum, but the chip
 * composable around it still carries Android-only `@Preview` tooling. The settings models depend
 * on this and nothing else from the chip.
 */
enum class ActiveState {
    Disabled, Default, Include, Exclude;

    fun click(): ActiveState {
        return when (this.ordinal) {
            Disabled.ordinal -> Disabled
            Default.ordinal -> Include
            Include.ordinal -> Exclude
            Exclude.ordinal -> Default
            else -> Default
        }
    }

    fun toBoolean(): Boolean? {
        return when (this.ordinal) {
            Disabled.ordinal -> null
            Default.ordinal -> null
            Include.ordinal -> true
            Exclude.ordinal -> false
            else -> null
        }
    }
}
