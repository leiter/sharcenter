package cut.the.crap.platform

/**
 * Desktop has no Toast, so messages are published as a flow for the Compose window to render as a
 * snackbar — see [FlowNotifier], which holds the whole implementation, and `App()`, which collects
 * it. The named subclass is kept so the Koin binding reads like its Android counterpart.
 */
class DesktopNotifier : FlowNotifier()
