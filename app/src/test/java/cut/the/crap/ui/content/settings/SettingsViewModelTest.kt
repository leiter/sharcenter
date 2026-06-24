package cut.the.crap.ui.content.settings

import app.cash.turbine.test
import cut.the.crap.fake.FakeSettingsRepository
import cut.the.crap.testutils.MainDispatcherRule
import cut.the.crap.testutils.TestData
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import cut.the.crap.data.preferences.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel
    private val settingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setup() {
        settingsRepository = mockk(relaxed = true)
        coEvery { settingsRepository.settingsFlow } returns settingsFlow
        viewModel = SettingsViewModel(settingsRepository)
    }

    @Test
    fun `initial state emits default AppSettings`() = runTest {
        viewModel.settings.test {
            val settings = awaitItem()
            assertThat(settings).isEqualTo(AppSettings())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `settings flow emits updated values`() = runTest {
        val newSettings = TestData.appSettings(
            postsDateRangePreset = DateRangePreset.THIRTY_DAYS,
            developerMode = true
        )

        viewModel.settings.test {
            assertThat(awaitItem()).isEqualTo(AppSettings())

            settingsFlow.value = newSettings

            assertThat(awaitItem()).isEqualTo(newSettings)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSettings calls repository`() = runTest {
        val newSettings = TestData.appSettings(developerMode = true)

        viewModel.updateSettings(newSettings)
        advanceUntilIdle()

        coVerify { settingsRepository.updateSettings(newSettings) }
    }

    @Test
    fun `updateDateRangePreset calls repository`() = runTest {
        val preset = DateRangePreset.NINETY_DAYS

        viewModel.updateDateRangePreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updateDateRangePreset(preset) }
    }

    @Test
    fun `updatePostsDateRangePreset calls repository`() = runTest {
        val preset = DateRangePreset.ALL_TIME

        viewModel.updatePostsDateRangePreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updatePostsDateRangePreset(preset) }
    }

    @Test
    fun `updateLinksDateRangePreset calls repository`() = runTest {
        val preset = DateRangePreset.SEVEN_DAYS

        viewModel.updateLinksDateRangePreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updateLinksDateRangePreset(preset) }
    }

    @Test
    fun `updatePostsFavoriteFilterPreset calls repository`() = runTest {
        val preset = FavoriteFilterPreset.FAVORITES_ONLY

        viewModel.updatePostsFavoriteFilterPreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updatePostsFavoriteFilterPreset(preset) }
    }

    @Test
    fun `updateLinksFavoriteFilterPreset calls repository`() = runTest {
        val preset = FavoriteFilterPreset.NON_FAVORITES_ONLY

        viewModel.updateLinksFavoriteFilterPreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updateLinksFavoriteFilterPreset(preset) }
    }

    @Test
    fun `updatePostsSortOrderPreset calls repository`() = runTest {
        val preset = SortOrderPreset.BY_DATE

        viewModel.updatePostsSortOrderPreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updatePostsSortOrderPreset(preset) }
    }

    @Test
    fun `updateLinksSortOrderPreset calls repository`() = runTest {
        val preset = SortOrderPreset.BY_ORDER

        viewModel.updateLinksSortOrderPreset(preset)
        advanceUntilIdle()

        coVerify { settingsRepository.updateLinksSortOrderPreset(preset) }
    }

    @Test
    fun `updateThemePreference calls repository`() = runTest {
        val theme = ThemePreference.DARK

        viewModel.updateThemePreference(theme)
        advanceUntilIdle()

        coVerify { settingsRepository.updateThemePreference(theme) }
    }

    @Test
    fun `toggleDeveloperMode enables when disabled`() = runTest {
        // viewModel settings start with developerMode = false (default)
        viewModel.toggleDeveloperMode()
        advanceUntilIdle()

        coVerify { settingsRepository.updateDeveloperMode(true) }
    }

    @Test
    fun `toggleDeveloperMode disables when enabled`() = runTest {
        // Set developerMode = true BEFORE creating viewModel
        settingsFlow.value = AppSettings(developerMode = true)

        // Create a new viewModel instance that will pick up the developerMode = true
        viewModel = SettingsViewModel(settingsRepository)
        advanceUntilIdle()

        // Wait for the settings StateFlow to receive the upstream value
        viewModel.settings.test {
            val settings = awaitItem()
            assertThat(settings.developerMode).isTrue()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.toggleDeveloperMode()
        advanceUntilIdle()

        coVerify { settingsRepository.updateDeveloperMode(false) }
    }
}
