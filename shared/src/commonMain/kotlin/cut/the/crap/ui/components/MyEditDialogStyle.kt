package cut.the.crap.ui.components

import androidx.compose.runtime.Immutable
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.ui.components.api.Action

/**
 * What an edit dialog is being opened *for* — the payload, not the dialog.
 *
 * Extracted from `MyEditDialog.kt` for the same reason as [FilterState]: it is a model that
 * happened to live beside the composable that renders it, and `api.Action` references it — so the
 * action contract could not reach `commonMain` while it was stranded in a Compose file.
 *
 * The rendering stays in `MyEditDialog.kt` until WP7.
 */
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
