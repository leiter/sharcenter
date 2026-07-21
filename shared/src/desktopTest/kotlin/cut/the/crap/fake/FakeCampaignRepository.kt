package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignRepository

/**
 * Serves whatever [campaign] is set to. The Posts tests don't exercise campaign loading, so the
 * default is an empty campaign rather than a `TODO()`.
 */
class FakeCampaignRepository(
    var campaign: Campaign = Campaign(
        name = "abu-safiya",
        version = 1,
        locateUrl = null,
        countries = emptyList()
    )
) : CampaignRepository {
    override suspend fun getCampaign(): Result<Campaign> = Result.Success(campaign)
}
