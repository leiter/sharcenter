package cut.the.crap.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

/**
 * Picks files with a [UIDocumentPickerViewController].
 *
 * Presented with **`asCopy = true`**: iOS copies each chosen file into the app's temp sandbox and
 * hands back an ordinary `file://` URL, *not* a security-scoped one. That is exactly what the
 * read-once import/restore paths want — [IosFileAccess] reads the copy as a plain path, and there is
 * no `startAccessingSecurityScopedResource` lifetime to manage across the async read. The tradeoff
 * (a temp-dir copy per pick) is irrelevant for the small CSV/DB files these callers handle.
 *
 * [mimeTypes] map to `UTType`s; a wildcard (the all-types glob, or a `type/`-prefix glob) or any
 * unmappable type falls back to [UTTypeItem] — "any file" — mirroring why the Android picker offers
 * the wildcard (see [rememberFilePicker]'s doc).
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    allowMultiple: Boolean,
    onPicked: (List<PlatformUri>) -> Unit,
): FilePickerLauncher {
    // The picker is presented later, from a click; read the latest callback rather than the one
    // captured at composition time (mirrors the desktop impl).
    val currentOnPicked by rememberUpdatedState(onPicked)

    // UIDocumentPickerViewController.delegate is a *weak* reference, so nothing else keeps the
    // delegate alive between presentation and its callback. Hold it here until we've heard back.
    val delegateHolder = remember { DelegateHolder() }
    val types = remember(mimeTypes) { mimeTypes.toContentTypes() }

    return remember(allowMultiple, types) {
        FilePickerLauncher {
            val delegate = PickerDelegate { uris ->
                delegateHolder.value = null // released once the pick (or cancel) has been reported
                currentOnPicked(uris)
            }
            delegateHolder.value = delegate

            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = types,
                asCopy = true,
            ).apply {
                this.delegate = delegate
                allowsMultipleSelection = allowMultiple
            }
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/** Strong holder for the otherwise-weakly-referenced picker delegate. */
private class DelegateHolder {
    var value: PickerDelegate? = null
}

private class PickerDelegate(
    private val onResult: (List<PlatformUri>) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val uris = didPickDocumentsAtURLs
            .mapNotNull { (it as? NSURL)?.absoluteString }
            .map(::PlatformUri)
        onResult(uris)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(emptyList())
    }
}

/**
 * MIME types to the `UTType`s the picker filters by. An empty list, a wildcard glob, or any type
 * UTType can't resolve collapses to [UTTypeItem] (any file), so the chooser never silently hides a
 * file the caller would have accepted.
 */
private fun List<String>.toContentTypes(): List<UTType> {
    if (isEmpty() || any { it == "*/*" || it.endsWith("/*") }) return listOf(UTTypeItem)
    val mapped = mapNotNull { UTType.typeWithMIMEType(it) }
    return mapped.ifEmpty { listOf(UTTypeItem) }
}
