package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.dialog_delete
import cut.the.crap.shared.resources.dialog_save
import cut.the.crap.shared.resources.edit_dialog_confirm_delete
import cut.the.crap.shared.resources.edit_dialog_description_label
import cut.the.crap.shared.resources.edit_dialog_hide
import cut.the.crap.shared.resources.edit_dialog_link_label
import cut.the.crap.shared.resources.edit_entity_title
import androidx.compose.ui.unit.dp
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.MiscAction

sealed interface MyEditDialogStyle {

    val title: String?
    val confirm: String?
    val dismiss: String?

    @Immutable
    data class OfferDelete(
        override val title: String? = "",
        override val confirm: String? = "Delete",
        override val dismiss: String? = "Cancel",
        val actionPayload: Action,
    ) : MyEditDialogStyle

    @Immutable
    data class ExportLinks(
        override val title: String? = "",
        override val confirm: String? = "Save",
        override val dismiss: String? = "Cancel",
        val actionPayload: Action,
    ) : MyEditDialogStyle

    @Immutable
    data class EditEntity(
        override val title: String? = "Edit entity",
        val tweetItem: ContentLink = ContentLink(),
        override val confirm: String? = null,
        override val dismiss: String? = null,
    ) : MyEditDialogStyle
}


@Composable
fun MyEditDialog(
    action: (Action) -> Unit,
    onDismissRequest: () -> Unit,
    style: MyEditDialogStyle = MyEditDialogStyle.EditEntity(),
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = style.renderTitle(),
        text = style.renderText(action = action),
        confirmButton = {
            TextButton(
                onClick = {
                    onSave()
                    onDismissRequest()
                }
            ) {
                val confirmRes = when (style) {
                    is MyEditDialogStyle.OfferDelete -> Res.string.dialog_delete
                    else -> Res.string.dialog_save
                }
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun MyEditDialogStyle.renderTitle(): (@Composable () -> Unit)? {
    return when (this) {
        is MyEditDialogStyle.EditEntity -> {
            { Text(text = stringResource(Res.string.edit_entity_title)) }
        }
        // Delete/Export styles show no dialog title.
        is MyEditDialogStyle.OfferDelete -> null
        is MyEditDialogStyle.ExportLinks -> null
    }
}

@Composable
private fun MyEditDialogStyle.renderText(action: (Action) -> Unit): (@Composable () -> Unit) {

    return {
        when (this) {
            is MyEditDialogStyle.EditEntity -> {
                Column {
                    OutlinedTextField(
                        value = tweetItem.link,
                        onValueChange = { /* TODO: Add link edit action */ },
                        label = { Text(stringResource(Res.string.edit_dialog_link_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tweetItem.description,
                        onValueChange = { /* TODO: Add description edit action */ },
                        label = { Text(stringResource(Res.string.edit_dialog_description_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Checkbox(
//                            checked = isChecked,
//                            onCheckedChange = onCheckChange
//                        )
                        IconButton(
                            onClick = {
                                action(MiscAction.SelectPressed(tweetItem))
                            },
                            ) { Icon(imageVector = Icons.Outlined.Check, contentDescription = "")
                        }
                        Text(text = stringResource(Res.string.edit_dialog_hide))
                    }
                }
            }
            is MyEditDialogStyle.OfferDelete -> { Text(text = stringResource(Res.string.edit_dialog_confirm_delete)) }

            is MyEditDialogStyle.ExportLinks -> TODO()
        }
    }
}

@Composable
private fun MyEditDialogStyle.renderConfirm(): @Composable () -> Unit {
    return {
        when (this) {
            is MyEditDialogStyle.EditEntity -> Unit
            else -> Unit
        }
    }
}

@Composable
private fun MyEditDialogStyle.renderDismiss(): (@Composable () -> Unit) {

    return {
        when (this) {
            is MyEditDialogStyle.EditEntity -> Unit
            else -> Unit

        }

    }
}
