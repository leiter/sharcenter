package cut.the.crap.ui.content.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.data.rest.campaign.CampaignSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CampaignListUiState(
    val loading: Boolean = true,
    val campaigns: List<CampaignSummary> = emptyList(),
    val error: AppError? = null,
    /**
     * True when the list is empty *and* the load succeeded — an empty answer from the server,
     * not a failed one. The two look identical in a bare list and mean opposite things.
     */
    val isEmpty: Boolean = false,
)

/**
 * Backs the campaign list — the screen that replaces the inert country list
 * (`CAMPAIGN_SCHEMA_SPEC.md` §6.3).
 */
class CampaignListViewModel(
    private val repository: CampaignRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CampaignListUiState())
    val state: StateFlow<CampaignListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            _state.value = when (val result = repository.list()) {
                is Result.Success -> CampaignListUiState(
                    loading = false,
                    campaigns = result.data,
                    isEmpty = result.data.isEmpty(),
                )
                is Result.Error -> CampaignListUiState(
                    loading = false,
                    // Keep whatever was already on screen: a failed refresh should not blank a
                    // list the user was reading.
                    campaigns = _state.value.campaigns,
                    error = result.error,
                )
            }
        }
    }
}
