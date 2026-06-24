package cut.the.crap.tools

import androidx.lifecycle.ViewModel
import cut.the.crap.ui.components.api.Screen
import cut.the.crap.ui.content.links.LinksViewModel
import cut.the.crap.ui.content.posts.PostsViewModel

/**
 * Extension property that defines which screens a ViewModel is responsible for handling.
 * This centralizes the screen routing logic and prevents code duplication.
 */
val ViewModel.handlesScreens: List<Screen>
    get() = when (this) {
        is LinksViewModel -> listOf(Screen.Links, Screen.Current)
        is PostsViewModel -> listOf(Screen.Posts, Screen.Current)
        else -> emptyList()
    }

/**
 * Check if this ViewModel handles actions for the given screen.
 */
fun ViewModel.isForThisScreen(screen: Screen): Boolean {
    return screen in handlesScreens
}