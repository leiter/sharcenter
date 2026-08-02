package cut.the.crap.ui.content.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CampaignDetailUiState(
    val loading: Boolean = true,
    val campaign: Campaign? = null,
    val error: AppError? = null,
) {
    /**
     * The operator disabled this campaign (C6). Distinct from any other error because it is the
     * one the user must not be told to retry — it will keep failing until a human decides
     * otherwise.
     */
    val isDisabled: Boolean get() = (error as? AppError.Client)?.status == 451
}

/**
 * Backs the campaign detail screen: the campaign's countries, its posts and its contact actions.
 *
 * Takes the id rather than the campaign so the screen is reachable by deep link and survives
 * process death — the previous country screen depended on a payload loaded by another view model
 * before navigation, and rendered "no data" whenever that assumption broke.
 */
class CampaignDetailViewModel(
    private val repository: CampaignRepository,
    private val campaignId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(CampaignDetailUiState())
    val state: StateFlow<CampaignDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            _state.value = when (val result = repository.get(campaignId, forceRefresh)) {
                is Result.Success -> CampaignDetailUiState(loading = false, campaign = result.data)
                is Result.Error -> CampaignDetailUiState(
                    loading = false,
                    campaign = _state.value.campaign,
                    error = result.error,
                )
            }
        }
    }
}
