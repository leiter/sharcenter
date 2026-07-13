package cut.the.crap.ui.content.links

import cut.the.crap.data.rest.AppError

/**
 * What the Links screen wants to tell the user, as data rather than prose.
 *
 * Same split as [cut.the.crap.data.rest.AppError]: the ViewModel says *what happened*, and the
 * UI ([localizedText]) decides how to phrase it. Keeping the wording out of the ViewModel is
 * what lets it move to `commonMain` — and it keeps the unit tests off Android resources, which
 * a plain JVM test JVM cannot load.
 */
sealed interface LinksSnackbar {

    // Bulk actions
    data class Deleted(val count: Int) : LinksSnackbar
    data class FavouritesChanged(val count: Int, val favourited: Boolean) : LinksSnackbar
    data class Tagged(val count: Int) : LinksSnackbar
    data class Submitted(val count: Int) : LinksSnackbar
    data class SubmitFailed(val error: AppError) : LinksSnackbar

    // Export
    data class ExportSucceeded(val count: Int) : LinksSnackbar
    data object ExportNoItemsSelected : LinksSnackbar
    data class ExportDelimiterConflict(val delimiter: String) : LinksSnackbar
    data object ExportUnknownError : LinksSnackbar
    data class ExportFailed(val detail: String) : LinksSnackbar

    // Import
    data class ImportSucceeded(
        val imported: Int,
        val skipped: Int,
        val failed: Int,
    ) : LinksSnackbar
    data object ImportCannotOpen : LinksSnackbar
    data object ImportEmptyFile : LinksSnackbar
    data object ImportNoValidItems : LinksSnackbar
    data class ImportFailed(val detail: String) : LinksSnackbar
}
