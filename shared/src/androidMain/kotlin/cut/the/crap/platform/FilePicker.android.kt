package cut.the.crap.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Picks files through the Storage Access Framework.
 *
 * Both contracts are registered unconditionally — `rememberLauncherForActivityResult` cannot be
 * called behind an `if`, because registration must be stable across recompositions. Which one is
 * actually launched is decided at click time.
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    allowMultiple: Boolean,
    onPicked: (List<PlatformUri>) -> Unit,
): FilePickerLauncher {
    val single = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        onPicked(listOfNotNull(uri?.toPlatformUri()))
    }

    val multiple = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        onPicked(uris.map { it.toPlatformUri() })
    }

    val types = remember(mimeTypes) { mimeTypes.toTypedArray() }

    return remember(allowMultiple, types, single, multiple) {
        FilePickerLauncher {
            if (allowMultiple) multiple.launch(types) else single.launch(types)
        }
    }
}
