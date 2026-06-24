package cut.the.crap.tools

/**
 * Parses and serializes structured description strings for ContentLink metadata.
 *
 * Format: [optional_header]<metadata><L1><handles><L3><hashtags><L3><keywords>
 *
 * - L1 separates metadata from tags
 * - L2 separates fields within metadata and items within each tag category
 * - L3 separates the three tag categories (handles, hashtags, keywords)
 *
 * Header format: %%<L1_index><L2_index><L3_index>%% declares which delimiter set each level uses.
 * No header means default delimiters (set 0). Legacy descriptions (no L1 delimiter) are treated
 * as comma-separated flat keywords.
 */
object DescriptionParser {

    data class Delimiters(val l1: String, val l2: String, val l3: String)

    val DELIMITER_SETS = listOf(
        Delimiters("@@", ":", ",,"),    // index 0 (default)
        Delimiters("||", ";", "::"),    // index 1
        Delimiters("##", "|", "~~"),    // index 2
        Delimiters("~~", "^", "##"),    // index 3
    )

    private val HEADER_REGEX = Regex("""^%%(\d)(\d)(\d)%%""")

    data class ParsedDescription(
        val metadata: List<String>,
        val handles: List<String>,
        val hashtags: List<String>,
        val keywords: List<String>,
        val delimiters: Delimiters,
        val isLegacy: Boolean,
    )

    /**
     * Parse a description string into structured components.
     *
     * 1. If starts with "%%" -> extract header (3 indices), get custom delimiters, strip header
     * 2. Else if contains "@@" -> default delimiters
     * 3. Else -> LEGACY mode: treat as comma-separated flat keywords
     */
    fun parse(description: String): ParsedDescription {
        if (description.isBlank()) {
            return ParsedDescription(
                metadata = emptyList(),
                handles = emptyList(),
                hashtags = emptyList(),
                keywords = emptyList(),
                delimiters = DELIMITER_SETS[0],
                isLegacy = true,
            )
        }

        // 1. Check for header
        val headerMatch = HEADER_REGEX.find(description)
        if (headerMatch != null) {
            val l1Idx = headerMatch.groupValues[1].toIntOrNull() ?: 0
            val l2Idx = headerMatch.groupValues[2].toIntOrNull() ?: 0
            val l3Idx = headerMatch.groupValues[3].toIntOrNull() ?: 0
            val delimiters = Delimiters(
                l1 = DELIMITER_SETS.getOrElse(l1Idx) { DELIMITER_SETS[0] }.l1,
                l2 = DELIMITER_SETS.getOrElse(l2Idx) { DELIMITER_SETS[0] }.l2,
                l3 = DELIMITER_SETS.getOrElse(l3Idx) { DELIMITER_SETS[0] }.l3,
            )
            val body = description.substring(headerMatch.range.last + 1)
            return parseStructured(body, delimiters)
        }

        // 2. Check for default L1 delimiter ("@@")
        val defaultDelimiters = DELIMITER_SETS[0]
        if (description.contains(defaultDelimiters.l1)) {
            return parseStructured(description, defaultDelimiters)
        }

        // 3. Legacy mode: comma-separated keywords
        val keywords = description.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return ParsedDescription(
            metadata = emptyList(),
            handles = emptyList(),
            hashtags = emptyList(),
            keywords = keywords,
            delimiters = defaultDelimiters,
            isLegacy = true,
        )
    }

    private fun parseStructured(body: String, delimiters: Delimiters): ParsedDescription {
        // Split on L1 -> metadata section + tags section
        val l1Parts = body.split(delimiters.l1, limit = 2)
        val metadataSection = l1Parts[0]
        val tagsSection = l1Parts.getOrElse(1) { "" }

        // Split metadata on L2 -> positional fields
        val metadata = if (metadataSection.isNotEmpty()) {
            metadataSection.split(delimiters.l2).map { it.trim() }
        } else {
            emptyList()
        }

        // Split tags section on L3 -> up to 3 categories
        val tagCategories = if (tagsSection.isNotEmpty()) {
            tagsSection.split(delimiters.l3, limit = 3)
        } else {
            emptyList()
        }

        // Split each category on L2 -> individual items
        val handles = tagCategories.getOrElse(0) { "" }
            .split(delimiters.l2).map { it.trim() }.filter { it.isNotEmpty() }
        val hashtags = tagCategories.getOrElse(1) { "" }
            .split(delimiters.l2).map { it.trim() }.filter { it.isNotEmpty() }
        val keywords = tagCategories.getOrElse(2) { "" }
            .split(delimiters.l2).map { it.trim() }.filter { it.isNotEmpty() }

        return ParsedDescription(
            metadata = metadata,
            handles = handles,
            hashtags = hashtags,
            keywords = keywords,
            delimiters = delimiters,
            isLegacy = false,
        )
    }

    /**
     * Serialize structured components back into a description string.
     *
     * If delimiters are provided, they are used directly. Otherwise, chooseDelimiters() picks
     * the safest set based on field contents.
     */
    fun serialize(
        metadata: List<String>,
        handles: List<String>,
        hashtags: List<String>,
        keywords: List<String>,
        delimiters: Delimiters? = null,
    ): String {
        val allFields = metadata + handles + hashtags + keywords
        val chosen = delimiters ?: chooseDelimiters(metadata, handles, hashtags, keywords)

        val metaStr = metadata.joinToString(chosen.l2)
        val handlesStr = handles.joinToString(chosen.l2)
        val hashtagsStr = hashtags.joinToString(chosen.l2)
        val keywordsStr = keywords.joinToString(chosen.l2)
        val tagsStr = listOf(handlesStr, hashtagsStr, keywordsStr).joinToString(chosen.l3)

        val body = if (metaStr.isNotEmpty() || tagsStr.replace(chosen.l3, "").isNotEmpty()) {
            "$metaStr${chosen.l1}$tagsStr"
        } else {
            return ""
        }

        // Check if we need a header (non-default delimiters)
        val header = buildHeader(chosen)
        return "$header$body"
    }

    /**
     * Strip delimiter characters from user/API input to prevent parsing conflicts.
     * Replaces delimiter chars with safe alternatives.
     */
    fun sanitize(input: String, delimiters: Delimiters): String {
        var result = input
        // Replace L1 delimiter
        result = result.replace(delimiters.l1, if (delimiters.l1 == "@@") "@" else delimiters.l1.first().toString())
        // Replace L2 delimiter
        result = result.replace(delimiters.l2, if (delimiters.l2 == ":") "-" else delimiters.l2.first().toString())
        // Replace L3 delimiter
        result = result.replace(delimiters.l3, if (delimiters.l3 == ",,") "," else delimiters.l3.first().toString())
        return result.trim()
    }

    /**
     * Choose the best delimiter set that avoids conflicts with all field content.
     * Tries each set (0-3) in order and returns the first with no conflicts.
     */
    fun chooseDelimiters(vararg fieldGroups: List<String>): Delimiters {
        val allContent = fieldGroups.flatMap { it }

        for (set in DELIMITER_SETS) {
            val hasConflict = allContent.any { field ->
                field.contains(set.l1) || field.contains(set.l2) || field.contains(set.l3)
            }
            if (!hasConflict) return set
        }
        // Fallback to last set if all have conflicts (shouldn't happen in practice)
        return DELIMITER_SETS.last()
    }

    /**
     * Build the %%XYZ%% header string for non-default delimiters, or empty string for defaults.
     */
    private fun buildHeader(delimiters: Delimiters): String {
        if (delimiters == DELIMITER_SETS[0]) return ""

        val l1Idx = DELIMITER_SETS.indexOfFirst { it.l1 == delimiters.l1 }.coerceAtLeast(0)
        val l2Idx = DELIMITER_SETS.indexOfFirst { it.l2 == delimiters.l2 }.coerceAtLeast(0)
        val l3Idx = DELIMITER_SETS.indexOfFirst { it.l3 == delimiters.l3 }.coerceAtLeast(0)

        return "%%$l1Idx$l2Idx$l3Idx%%"
    }
}
