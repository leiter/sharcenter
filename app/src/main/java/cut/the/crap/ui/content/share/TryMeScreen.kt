package cut.the.crap.ui.content.share

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import cut.the.crap.intent.TwitterIntent
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.intent.extractTweetId
import cut.the.crap.data.storage.FileHelper
import cut.the.crap.tools.CONTENT
import cut.the.crap.tools.TWITTER_PACKAGE
import cut.the.crap.tools.InstalledCheck
import cut.the.crap.ui.components.BottomNavigationBar
import cut.the.crap.ui.components.MySpeedDialFab
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.content.links.LinksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryMeScreen(
    action: (Action) -> Unit,
    navController: NavHostController,
    consumeAction: LinksViewModel,
) {
    Scaffold(
    topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text("")
            },
            actions = {

            },
            navigationIcon = {
//                FilledTonalIconButton(
//                    onClick = {
////                            showScreen(Screen.WellnessListOrigin)
//                    }) {
//                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
//                }
            }
        )
    },
    bottomBar = {
        BottomNavigationBar(navController = navController)
    },
    floatingActionButton = {
        MySpeedDialFab()
    }
    ) {
        var tweetIdString by remember {
            mutableStateOf("")
        }
        var tweetComment by remember {
            mutableStateOf("")
        }

        var tweetUrl by remember {
            mutableStateOf("")
        }

        val context = LocalContext.current
        val strings = remember {
            FileHelper(context).readFile(CONTENT).split("\n")
        }

        val activity = LocalActivity.current

        val updateTextField: (String, Int) -> Unit = { text, id ->
            when (id) {
                TWEET_ID -> tweetIdString = text.extractTweetId()
                TWEET_COMMENT -> tweetComment= text
                TWEET_URL_PARAM -> tweetUrl = text
                else -> Unit
            }
        }

        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val customButtonModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
            ElevatedButton(
                modifier = customButtonModifier,
                onClick = {
                    strings.map { url ->
                        consumeAction.insertContentLink(
                            ContentLink(
                                link = url,
                            )
                        )
                    }
                }
            ) {
                Text(text = "Export Database plain")
            }
            ElevatedButton(
                modifier = customButtonModifier,
                onClick = {

                }
            ) {
                Text(text = "Export Database table")
            }
            ElevatedButton(
                modifier = customButtonModifier,
                onClick = {

                }
            ) {
                Text(text = "Import Lines")
            }
            ElevatedButton(
                modifier = customButtonModifier,
                onClick = {

                }
            ) {
                Text(text = "Import table")
            }

            Spacer(modifier = Modifier.height(144.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = tweetIdString,
                    onValueChange = { id ->
                        updateTextField(id, TWEET_ID)
                    },
                    modifier = Modifier.width(144.dp),
                    maxLines = 1,
                    placeholder = { Text(text = "Tweet id")}
                )
                TextField(
                    value = tweetUrl,
                    onValueChange = { id ->
                        updateTextField(id, TWEET_URL_PARAM)
                    },
                    maxLines = 1,
                    placeholder = { Text(text = "Tweet url param")}

                )
            }

            TextField(
                value = tweetComment,
                onValueChange = { id ->
                    updateTextField(id, TWEET_COMMENT)
                },
                modifier = Modifier
                    .height(144.dp)
                    .fillMaxWidth(),
                placeholder = { Text(text = "Tweet comment")}
            )

            TextButton(
                onClick = {
                    val tweet = TwitterIntent.Retweet(tweetIdString)
                    activity?.tweet(tweet) ?: Log.w("TryMeScreen", "Retweet didn't happen.")
                }
            ) {
                Text(text = "Retweet")
            }
            TextButton(
                onClick = {
                    activity?.tweet(TwitterIntent.QuoteTweet(tweetIdString, tweetComment))
                        ?: Log.w("TryMeScreen", "Quote didn't happen.")
            }) {
                Text(text = "Quote Tweet")
            }

            TextButton(
                onClick = {
                    val tweet = TwitterIntent.PostTweet(tweetComment, tweetUrl)
                    activity?.tweet(tweet) ?: Log.w("TryMeScreen", "Post didn't happen.")
            }) {
                Text(text = "Post Tweet")
            }

        }
    }
}

private const val TWEET_ID = 0
private const val TWEET_COMMENT = 1
private const val TWEET_URL_PARAM = 2

private fun Activity.tweet(twitterIntent: TwitterIntent) {
    if (InstalledCheck.isTwitterInstalled(this)) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = twitterIntent.url.toUri()
        intent.setPackage(TWITTER_PACKAGE)
        startActivity(intent, null)
    } else {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = twitterIntent.url.toUri()
        startActivity(intent, null)
    }
}
