package cut.the.crap.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.bluesky
import cut.the.crap.shared.resources.facebook
import cut.the.crap.shared.resources.img_not_available
import cut.the.crap.shared.resources.instagram
import cut.the.crap.shared.resources.mastodon
import cut.the.crap.shared.resources.reddit
import cut.the.crap.shared.resources.tiktok
import cut.the.crap.shared.resources.x
import cut.the.crap.shared.resources.youtube


@Composable
fun domainPainter(domain: String) : Painter {
    return when(domain){
        "x" -> painterResource(Res.drawable.x)
        "youtube" -> painterResource(Res.drawable.youtube)
        "instagram" -> painterResource(Res.drawable.instagram)
        "facebook" -> painterResource(Res.drawable.facebook)
        "bsky" -> painterResource(Res.drawable.bluesky)
        "mastodon" -> painterResource(Res.drawable.mastodon)
        "tiktok" -> painterResource(Res.drawable.tiktok)
        "reddit" -> painterResource(Res.drawable.reddit)

        else -> painterResource(Res.drawable.img_not_available)
    }
}
