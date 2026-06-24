package cut.the.crap.ui.content.posts

import androidx.lifecycle.viewModelScope
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordType
import kotlinx.coroutines.launch

/**
 * Extension functions for managing keywords (handles, tags, keywords) in PostsViewModel
 */

/**
 * Adds a new handle (account/username) to the database
 */
internal fun PostsViewModel.addAccount(text: String) {
    viewModelScope.launch {
        val handle = KeyWord(text = text, type = KeywordType.ACCOUNT)
        keywordRepository.insert(handle)
    }
}

/**
 * Adds a new tag (hashtag) to the database
 */
internal fun PostsViewModel.addHashtag(text: String) {
    viewModelScope.launch {
        val tag = KeyWord(text = text, type = KeywordType.HASHTAG)
        keywordRepository.insert(tag)
    }
}

/**
 * Adds a new keyword to the database
 */
internal fun PostsViewModel.addTag(text: String) {
    viewModelScope.launch {
        val keyWord = KeyWord(text = text, type = KeywordType.TAG)
        keywordRepository.insert(keyWord)
    }
}

/**
 * Imports multiple handles from a list of strings
 */
internal fun PostsViewModel.importAccountsFromList(handles: List<String>) {
    viewModelScope.launch {
        keywordRepository.importFromList(handles, KeywordType.ACCOUNT)
    }
}

/**
 * Imports multiple tags from a list of strings
 */
internal fun PostsViewModel.importHashtagsFromList(tags: List<String>) {
    viewModelScope.launch {
        keywordRepository.importFromList(tags, KeywordType.HASHTAG)
    }
}
