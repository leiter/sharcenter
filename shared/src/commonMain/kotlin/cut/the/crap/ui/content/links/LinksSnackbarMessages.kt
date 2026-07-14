package cut.the.crap.ui.content.links

import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.links_export_delimiter_conflict
import cut.the.crap.shared.resources.links_export_no_items_selected
import cut.the.crap.shared.resources.links_export_success
import cut.the.crap.shared.resources.links_export_unknown_error
import cut.the.crap.shared.resources.links_import_cannot_open
import cut.the.crap.shared.resources.links_import_empty_file
import cut.the.crap.shared.resources.links_import_failed_count
import cut.the.crap.shared.resources.links_import_imported
import cut.the.crap.shared.resources.links_import_no_valid_items
import cut.the.crap.shared.resources.links_import_skipped
import cut.the.crap.shared.resources.links_snackbar_deleted
import cut.the.crap.shared.resources.links_snackbar_export_failed
import cut.the.crap.shared.resources.links_snackbar_favorited
import cut.the.crap.shared.resources.links_snackbar_import_failed
import cut.the.crap.shared.resources.links_snackbar_submit_failed
import cut.the.crap.shared.resources.links_snackbar_submitted
import cut.the.crap.shared.resources.links_snackbar_tagged
import cut.the.crap.shared.resources.links_snackbar_unfavorited
import cut.the.crap.ui.localizedText
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Phrases a [LinksSnackbar] for the user. The UI half of the split — see [LinksSnackbar].
 *
 * Suspending because Compose resources are read outside composition here: the Links screen
 * collects these events in a `LaunchedEffect` and hands them to `showSnackbar`.
 */
suspend fun LinksSnackbar.localizedText(): String = when (this) {
    // Bulk actions
    is LinksSnackbar.Deleted ->
        getPluralString(Res.plurals.links_snackbar_deleted, count, count)

    is LinksSnackbar.FavouritesChanged -> getPluralString(
        if (favourited) Res.plurals.links_snackbar_favorited
        else Res.plurals.links_snackbar_unfavorited,
        count, count,
    )

    is LinksSnackbar.Tagged ->
        getPluralString(Res.plurals.links_snackbar_tagged, count, count)

    is LinksSnackbar.Submitted ->
        getPluralString(Res.plurals.links_snackbar_submitted, count, count)

    is LinksSnackbar.SubmitFailed ->
        getString(Res.string.links_snackbar_submit_failed, error.localizedText())

    // Export
    is LinksSnackbar.ExportSucceeded ->
        getPluralString(Res.plurals.links_export_success, count, count)

    LinksSnackbar.ExportNoItemsSelected ->
        exportFailed(getString(Res.string.links_export_no_items_selected))

    is LinksSnackbar.ExportDelimiterConflict ->
        exportFailed(getString(Res.string.links_export_delimiter_conflict, delimiter))

    LinksSnackbar.ExportUnknownError ->
        exportFailed(getString(Res.string.links_export_unknown_error))

    is LinksSnackbar.ExportFailed -> exportFailed(detail)

    // Import
    is LinksSnackbar.ImportSucceeded -> buildString {
        append(getPluralString(Res.plurals.links_import_imported, imported, imported))
        if (skipped > 0) append(getPluralString(Res.plurals.links_import_skipped, skipped, skipped))
        if (failed > 0) append(getPluralString(Res.plurals.links_import_failed_count, failed, failed))
    }

    LinksSnackbar.ImportCannotOpen ->
        importFailed(getString(Res.string.links_import_cannot_open))

    LinksSnackbar.ImportEmptyFile ->
        importFailed(getString(Res.string.links_import_empty_file))

    LinksSnackbar.ImportNoValidItems ->
        importFailed(getString(Res.string.links_import_no_valid_items))

    is LinksSnackbar.ImportFailed -> importFailed(detail)
}

/** Both failure strings read "Export/Import failed: <reason>", so the reason is nested. */
private suspend fun exportFailed(reason: String) =
    getString(Res.string.links_snackbar_export_failed, reason)

private suspend fun importFailed(reason: String) =
    getString(Res.string.links_snackbar_import_failed, reason)
