package cut.the.crap.ui.content.links

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.domain.DELIMITER
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.toContentLink
import cut.the.crap.data.domain.toLine
import cut.the.crap.data.domain.validateDelimiter
import cut.the.crap.data.rest.YouTubeUrlParser
import cut.the.crap.data.storage.saveFileToDownloads
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.parseSocialMediaUrl
import cut.the.crap.ui.components.ActiveState
import cut.the.crap.ui.components.FilterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

/**
 * Extension functions for handling import/export operations in LinksViewModel
 */

internal fun LinksViewModel.exportSelectedItems(outputStream: OutputStream?): LinksSnackbar {
    return try {
        // Get selected items
        val selectedIds = internalScreenState.value.selectedItems
        val itemsToExport = listState.value.filter { selectedIds.contains(it.id) }

        if (itemsToExport.isEmpty()) {
            return LinksSnackbar.ExportNoItemsSelected
        }

        // Validate delimiter
        if (!itemsToExport.validateDelimiter()) {
            return LinksSnackbar.ExportDelimiterConflict(DELIMITER)
        }

        // Build export content
        val exportContent = buildExportContent(
            items = itemsToExport,
            filterState = internalScreenState.value
        )

        // Write to file
        val result = saveFileToDownloads(outputStream, exportContent)

        if (result.isSuccess) {
            LinksSnackbar.ExportSucceeded(itemsToExport.size)
        } else {
            result.exceptionOrNull()?.message
                ?.let { LinksSnackbar.ExportFailed(it) }
                ?: LinksSnackbar.ExportUnknownError
        }
    } catch (e: Exception) {
        LinksSnackbar.ExportFailed(e.message ?: "")
    }
}

internal fun buildExportContent(
    items: List<ContentLink>,
    filterState: LinksScreenState
): String {
    val lines = mutableListOf<String>()

    // Add data lines
    items.forEach { item ->
        lines.add(item.toLine())
    }

    // Add metadata section
    lines.add("")
    lines.add("# METADATA")
    lines.add("# export_timestamp: ${System.currentTimeMillis()}")
    lines.add("# db_version: 4")
    lines.add("# format_version: 1")
    lines.add("# total_items: ${items.size}")
    lines.add("# delimiter: $DELIMITER")
    lines.add("# columns: id,link,description,added,position,favourite,hideItem")

    // Add filter information
    val filterInfo = buildFilterInfo(filterState)
    if (filterInfo.isNotEmpty()) {
        lines.add("# filters: $filterInfo")
    }

    lines.add("# app_version: 1.0")
    lines.add("# export_source: Links Screen")

    return lines.joinToString("\n")
}

internal fun LinksViewModel.importFromFile(uri: Uri, context: Context): LinksSnackbar {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return LinksSnackbar.ImportCannotOpen

        val content = inputStream.bufferedReader().use { it.readText() }
        val lines = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        if (lines.isEmpty()) {
            return LinksSnackbar.ImportEmptyFile
        }

        // Get existing items to check for duplicates
        val existingItems = listState.value

        // Parse and import each line
        var importedCount = 0
        var skippedCount = 0
        var errorCount = 0

        lines.forEach { line ->
            try {
                val contentLink = line.toContentLink()

                // Check if item with same link already exists
                val isDuplicate = existingItems.any { it.link == contentLink.link }

                if (isDuplicate) {
                    skippedCount++
                } else {
                    viewModelScope.launch {
                        contentRepository.insert(contentLink)
                    }
                    importedCount++
                }
            } catch (e: Exception) {
                errorCount++
            }
        }

        // Batch-fetch YouTube metadata for imported links
        if (importedCount > 0) {
            val youtubeUrls = mutableListOf<String>()
            lines.forEach { line ->
                try {
                    val contentLink = line.toContentLink()
                    if (YouTubeUrlParser.isYouTubeUrl(contentLink.link)) {
                        youtubeUrls.add(contentLink.link)
                    }
                } catch (_: Exception) { }
            }
            if (youtubeUrls.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    // Query all recent items directly from DB
                    val now = System.currentTimeMillis()
                    val allItems = contentRepository.byTimeRange(now - 60_000, now + 1000)
                    youtubeUrls.forEach { url ->
                        val dbItem = allItems.firstOrNull { it.link == url } ?: return@forEach
                        val result = youTubeRepository.getVideoMetadata(url)
                        if (result is cut.the.crap.data.rest.Result.Success) {
                            val type = parseSocialMediaUrl(url)?.contentType ?: "video"
                            val updated = LinkMetadata.setYouTubeMetadata(
                                dbItem,
                                result.data.channelName,
                                result.data.title,
                                result.data.thumbnailUrl,
                                type
                            )
                            contentRepository.update(updated)
                        }
                    }
                }
            }
        }

        if (importedCount > 0 || skippedCount > 0) {
            LinksSnackbar.ImportSucceeded(
                imported = importedCount,
                skipped = skippedCount,
                failed = errorCount,
            )
        } else {
            LinksSnackbar.ImportNoValidItems
        }
    } catch (e: Exception) {
        LinksSnackbar.ImportFailed(e.message ?: "")
    }
}

internal fun buildFilterInfo(state: LinksScreenState): String {
    val filters = mutableListOf<String>()

    // Check favorite filter
    val favoriteFilter = state.filterStateList.filterIsInstance<FilterState.TripleState>()
        .firstOrNull()
    favoriteFilter?.let {
        when (it.activeState) {
            ActiveState.Include -> filters.add("favorites_only")
            ActiveState.Exclude -> filters.add("exclude_favorites")
            ActiveState.Default -> {} // No filter
            ActiveState.Disabled -> {} // No filter
        }
    }

    // Check date filters
    if (state.startTime != null && state.endTime != null) {
        filters.add("date_range:${state.startTime}-${state.endTime}")
    } else if (state.startTime != null) {
        filters.add("start_date:${state.startTime}")
    } else if (state.endTime != null) {
        filters.add("end_date:${state.endTime}")
    }

    return filters.joinToString(", ")
}
