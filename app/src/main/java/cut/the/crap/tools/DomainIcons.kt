package cut.the.crap.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import cut.the.crap.R


@Composable
fun domainPainter(domain: String) : Painter {
    return when(domain){
        "x" -> painterResource(id = R.drawable.x)
        "youtube" -> painterResource(id = R.drawable.youtube)
        "instagram" -> painterResource(id = R.drawable.instagram)
        "facebook" -> painterResource(id = R.drawable.facebook)
        "bsky" -> painterResource(id = R.drawable.bluesky)
        "mastodon" -> painterResource(id = R.drawable.mastodon)
        "tiktok" -> painterResource(id = R.drawable.tiktok)

        else -> rememberVectorPainter(
            ImageVector.vectorResource(id = R.drawable.img_not_available)
        )
    }
}
