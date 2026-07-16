package cut.the.crap.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS file picker — **not yet wired**.
 *
 * The real implementation presents a `UIDocumentPickerViewController` and hands back its
 * security-scoped `NSURL`s. That needs *presentation* testing to verify — not just compilation —
 * so it lands at app bring-up (WP-iOS-6/7). Until then the launcher yields nothing, which the
 * reduced iOS v1 tolerates: the only callers are the import/restore paths, and backup/restore is
 * capability-flagged off on iOS.
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    allowMultiple: Boolean,
    onPicked: (List<PlatformUri>) -> Unit,
): FilePickerLauncher = remember { FilePickerLauncher { onPicked(emptyList()) } }
