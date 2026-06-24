package cut.the.crap.tools

import androidx.compose.runtime.Immutable

/**
 * Wrapper for text field value that separates text content from selection state.
 * Used throughout the app to manage text input with cursor position.
 */
@Immutable
data class TextValueWrapper(
    val newText: String = "",
    val selection: Pair<Int, Int> = Pair(0, 0),
)

/**
 * Inserts text at the current cursor position with smart spacing.
 * Automatically adds spaces before and after the inserted content if needed.
 *
 * @param content The text to insert at the cursor position
 * @return A new TextValueWrapper with the inserted text and updated cursor position
 */
fun TextValueWrapper.insertText(content: String): TextValueWrapper {
    val cursorPosition = this.selection.first

    val original = this.newText.substring(0, cursorPosition)
    val ensuredTrailing = original.ensureTrailingSpace()
    val updatedText = ensuredTrailing +
        content.ensureTrailingSpace() +
        this.newText.substring(cursorPosition)

    val pos = cursorPosition + content.length + 1 + if (original == ensuredTrailing) 0 else 1
    return this.copy(
        newText = updatedText,
        selection = Pair(pos, pos)
    )
}
