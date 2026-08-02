package cut.the.crap.platform

/** Why the login screen is being shown — it changes the wording the user sees. */
enum class LoginReason {
    InitialSetup,
    SessionExpired,
}

/**
 * The interactive X sign-in flow.
 *
 * This is the seam that *cannot* be made to work everywhere, and it is modelled honestly rather
 * than faked. Signing in scrapes the session cookies out of a WebView; there is no WebView on a
 * desktop JVM, and no equivalent worth pretending about. So the capability is declared: the UI
 * asks [isSupported] and **hides the affordance** when it is false, instead of offering a button
 * that does nothing. Manual credential entry stays available on every platform as the fallback.
 *
 * There is no result to await. The flow persists the credentials it obtains to the settings
 * repository, and the settings screen already renders from that state, so the "logged in" row
 * updates itself. That keeps the seam a one-way [launch] and avoids dragging Android's
 * activity-result plumbing into common code.
 */
interface LoginFlow {
    /** Whether this platform can run the sign-in flow at all. */
    val isSupported: Boolean

    /** Starts the flow. A no-op when [isSupported] is false. */
    fun launch(reason: LoginReason)
}
