package cut.the.crap.ui.content.campaign

import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.fake.FakeCampaignRepository
import cut.the.crap.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The campaign list.
 *
 * The distinction that earns its keep here is **empty versus failed**. Both produce a screen with
 * no campaigns on it, and they call for opposite things: "you have not joined anything yet" is a
 * finished thought, while "we could not reach the server" needs a retry button. The old screen
 * collapsed every outcome into one "No campaign data available."
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun `loads the list on creation`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository(
            summaries = listOf(
                FakeCampaignRepository.summary("abu-safiya", "Abu Safiya", featured = true),
                FakeCampaignRepository.summary("mine", "Mine", role = "owner"),
            )
        )

        val model = CampaignListViewModel(repository)
        advanceUntilIdle()

        val state = model.state.value
        assertTrue(!state.loading)
        assertEquals(listOf("abu-safiya", "mine"), state.campaigns.map { it.id })
        assertTrue(!state.isEmpty)
        assertNull(state.error)
    }

    @Test
    fun `an empty list is empty, not an error`() = runTest(testDispatcher) {
        val model = CampaignListViewModel(FakeCampaignRepository(summaries = emptyList()))
        advanceUntilIdle()

        assertTrue(model.state.value.isEmpty)
        assertNull(model.state.value.error, "nothing failed — there is simply nothing yet")
    }

    @Test
    fun `a failed load is an error, not an empty list`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository().apply {
            failure = Result.Error(AppError.Network("no route"), retryable = true)
        }

        val model = CampaignListViewModel(repository)
        advanceUntilIdle()

        assertTrue(!model.state.value.isEmpty, "a failure must not read as 'you have none'")
        assertEquals(AppError.Network("no route"), model.state.value.error)
    }

    @Test
    fun `a failed refresh keeps the list that is already on screen`() = runTest(testDispatcher) {
        val repository = FakeCampaignRepository(
            summaries = listOf(FakeCampaignRepository.summary("abu-safiya"))
        )
        val model = CampaignListViewModel(repository)
        advanceUntilIdle()

        repository.failure = Result.Error(AppError.Network("dropped"), retryable = true)
        model.refresh()
        advanceUntilIdle()

        assertEquals(
            listOf("abu-safiya"),
            model.state.value.campaigns.map { it.id },
            "blanking a list the user was reading is worse than showing it slightly stale",
        )
        assertEquals(AppError.Network("dropped"), model.state.value.error)
    }

    @Test
    fun `a summary knows whether it is the user's own`() = runTest(testDispatcher) {
        val model = CampaignListViewModel(
            FakeCampaignRepository(
                summaries = listOf(
                    FakeCampaignRepository.summary("bundled", featured = true),
                    FakeCampaignRepository.summary("joined", role = "member"),
                )
            )
        )
        advanceUntilIdle()

        val (bundled, joined) = model.state.value.campaigns
        assertTrue(!bundled.isMine, "the bundled campaign is not something the user joined")
        assertTrue(bundled.featured)
        assertTrue(joined.isMine)
    }
}
