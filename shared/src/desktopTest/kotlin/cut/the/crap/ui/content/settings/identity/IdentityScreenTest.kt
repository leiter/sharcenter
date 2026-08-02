package cut.the.crap.ui.content.settings.identity

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import cut.the.crap.fake.FakeIdentityKeyStore
import cut.the.crap.fake.FakeIdentityRepository
import cut.the.crap.identity.IdentityManager
import cut.the.crap.platform.JvmCryptoProvider
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.rest.identity.IdentityRepository
import cut.the.crap.platform.FlowNotifier
import cut.the.crap.platform.Notifier
import kotlinx.coroutines.runBlocking
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import kotlin.test.Test

/**
 * Renders the identity screen for real.
 *
 * The view-model tests prove the logic; this proves the screen composes at all, which is the risk
 * with a large Compose file written without a device to try it on. It also pins the two things a
 * user would notice immediately if they broke: which sections appear in which server state, and
 * that nothing destructive happens on a single tap.
 */
@OptIn(ExperimentalTestApi::class)
class IdentityScreenTest {

    private val crypto = JvmCryptoProvider()

    // Only the Notifier goes through Koin — the view model is passed in directly, so the test
    // does not depend on a ViewModelStoreOwner being present in a bare composition.
    private val notifierModule = module { single<Notifier> { FlowNotifier() } }

    private fun ComposeUiTest.scrollTo(text: String) {
        onNodeWithTag(IDENTITY_LIST_TAG).performScrollToNode(hasText(text))
    }

    @Test
    fun `an install with no identity offers to create one`() = runComposeUiTest {
        val manager = IdentityManager(FakeIdentityKeyStore(), crypto)
        val repository = FakeIdentityRepository()

        setContent {
            KoinApplication(application = { modules(notifierModule) }) {
                IdentityScreen(
                    navController = rememberNavController(),
                    viewModel = IdentityViewModel(manager, repository),
                )
            }
        }
        waitForIdle()

        onNodeWithText("No identity yet").assertIsDisplayed()
        onNodeWithText("Create identity").assertIsDisplayed()
    }

    @Test
    fun `a registered install shows its user, devices and recovery phrase section`() =
        runComposeUiTest {
            val manager = IdentityManager(FakeIdentityKeyStore(), crypto)
            runBlocking { manager.getOrCreate() }
            val repository = FakeIdentityRepository.registered(
                FakeIdentityRepository.key("k1", "Phone"),
                FakeIdentityRepository.key("k2", "Laptop", revokedAt = 500),
            )

            setContent {
                KoinApplication(application = { modules(notifierModule) }) {
                    IdentityScreen(
                        navController = rememberNavController(),
                        viewModel = IdentityViewModel(manager, repository),
                    )
                }
            }
            waitForIdle()

            onNodeWithText("user-1").assertIsDisplayed()
            onNodeWithText("Phone").assertIsDisplayed()
            // The revoked device stays listed, with its revocation shown rather than hidden.
            onNodeWithText("Laptop").assertIsDisplayed()

            // The rest is below the fold of any test window; the list is scrollable.
            scrollTo("Recovery phrase")
            onNodeWithText("Recovery phrase").assertIsDisplayed()
            scrollTo("Reset identity")
            onNodeWithText("Reset identity").assertIsDisplayed()
            scrollTo("Reset")
            onNodeWithText("Reset").assertIsDisplayed()
        }

    @Test
    fun `showing the recovery phrase takes a confirmation, not one tap`() = runComposeUiTest {
        val manager = IdentityManager(FakeIdentityKeyStore(), crypto)
        runBlocking { manager.getOrCreate() }
        val repository = FakeIdentityRepository.registered(FakeIdentityRepository.key("k1"))

        setContent {
            KoinApplication(application = { modules(notifierModule) }) {
                IdentityScreen(
                    navController = rememberNavController(),
                    viewModel = IdentityViewModel(manager, repository),
                )
            }
        }
        waitForIdle()

        scrollTo("Show recovery phrase")
        onNodeWithText("Show recovery phrase").performClick()
        waitForIdle()

        // The dialog, not the words. The first 24-word render must never be one tap away.
        onNodeWithText("Show your recovery phrase?").assertIsDisplayed()
    }

    @Test
    fun `resetting asks first and says what is lost`() = runComposeUiTest {
        val manager = IdentityManager(FakeIdentityKeyStore(), crypto)
        runBlocking { manager.getOrCreate() }
        val repository = FakeIdentityRepository.registered(FakeIdentityRepository.key("k1"))

        setContent {
            KoinApplication(application = { modules(notifierModule) }) {
                IdentityScreen(
                    navController = rememberNavController(),
                    viewModel = IdentityViewModel(manager, repository),
                )
            }
        }
        waitForIdle()

        scrollTo("Reset identity")
        // The button, not the section header — they used to share a label, which is exactly the
        // ambiguity a user faces too.
        onNodeWithText("Reset").performClick()
        waitForIdle()

        onNodeWithText("Reset your identity?").assertIsDisplayed()
        onNodeWithText("permanently unmanageable", substring = true).assertIsDisplayed()
        // Still there: confirming is a separate, deliberate tap.
        assert(runBlocking { manager.current() } != null)
    }
}
