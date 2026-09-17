package cut.the.crap.ui.content.links

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.links_add_link_field
import cut.the.crap.shared.resources.links_add_link_placeholder
import cut.the.crap.shared.resources.links_add_link_save
import cut.the.crap.shared.resources.links_add_link_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The "paste a link" dialog: manual entry for a link that never went through the Android share
 * sheet — e.g. one copied in a desktop browser. [initialText] pre-fills the field with whatever
 * the clipboard held at open time, when [LinksScreen] judged it worth offering.
 *
 * Deliberately dumb: it does not itself decide what the text is (a URL vs an `@handle`) — that
 * happens once, in `ListAction.AddLink`'s handler, reusing exactly what the share sheet already
 * does with a shared URL. See `SharedUrlProcessor`.
 */
@Composable
fun AddLinkDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (text: String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = stringResource(Res.string.links_add_link_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    label = { Text(stringResource(Res.string.links_add_link_field)) },
                    placeholder = { Text(stringResource(Res.string.links_add_link_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (text.isNotBlank()) onSave(text) },
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                        Text(stringResource(Res.string.links_add_link_save))
                    }
                }
            }
        }
    }

    // Focus (and raise the keyboard) once, right after the dialog's first frame — requesting it
    // immediately can miss the field before Compose has finished laying it out.
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboard?.show()
    }
}
