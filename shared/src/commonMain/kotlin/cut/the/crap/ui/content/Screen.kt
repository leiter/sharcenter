package cut.the.crap.ui.content

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.screen_links
import cut.the.crap.shared.resources.screen_posts
import cut.the.crap.shared.resources.screen_profile
import org.jetbrains.compose.resources.StringResource

/**
 * The three bottom-navigation destinations and their routes. Referenced from the app-root
 * `handleAction` (commonMain) and `NavigationGraph` / `MyBottomBar` (still in :app until WP7),
 * so it lives here where every target can see it — it depends only on [ImageVector] and
 * [StringResource], both multiplatform.
 */
@Immutable
sealed class Screen(val route: String, val icon: ImageVector, val title: StringResource) {
    @Immutable
    data object Home : Screen("home", Icons.AutoMirrored.Default.Send, Res.string.screen_posts)
    @Immutable
    data object Search : Screen("search", Icons.Default.Link, Res.string.screen_links)
    @Immutable
    data object Profile : Screen("profile", Icons.Default.Settings, Res.string.screen_profile)
}
