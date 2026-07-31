package cut.the.crap.fake

import cut.the.crap.data.rest.AppError
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.Source
import cut.the.crap.data.rest.campaign.Campaign
import cut.the.crap.data.rest.campaign.CampaignRepository
import cut.the.crap.data.rest.campaign.CampaignSummary

/**
 * Serves whatever it is set to. The Posts tests don't exercise campaign loading, so the defaults
 * are empty rather than a `TODO()`.
 */
class FakeCampaignRepository(
    var campaign: Campaign = Campaign(
        name = "abu-safiya",
        version = 1,
        locateUrl = null,
        countries = emptyList()
    ),
    var summaries: List<CampaignSummary> = emptyList(),
) : CampaignRepository {

    /** Set to make the next calls fail. Cleared by hand, not automatically. */
    var failure: Result.Error? = null

    val requestedIds = mutableListOf<String>()

    /** Ids asked for with `forceRefresh = true` — how a test tells a reload from a cache hit. */
    val forcedIds = mutableListOf<String>()

    override suspend fun list(): Result<List<CampaignSummary>> =
        failure ?: Result.Success(summaries)

    override suspend fun get(id: String, forceRefresh: Boolean): Result<Campaign> {
        requestedIds += id
        if (forceRefresh) forcedIds += id
        return failure ?: Result.Success(campaign)
    }

    override suspend fun getCampaign(): Result<Campaign> = failure ?: Result.Success(campaign)

    companion object {
        fun summary(
            id: String,
            title: String = id,
            role: String? = null,
            featured: Boolean = false,
            countryCount: Int = 0,
            postCount: Int = 0,
            contactCount: Int = 0,
        ) = CampaignSummary(
            id = id,
            title = title,
            description = null,
            state = "active",
            version = 1,
            role = role,
            featured = featured,
            countryCount = countryCount,
            postCount = postCount,
            contactCount = contactCount,
        )

        fun notFound() = Result.Error(AppError.NotFound(Source.CAMPAIGN), retryable = false)

        /** What the server sends when the operator pulled the kill switch (C6). */
        fun disabled() = Result.Error(AppError.Client(451, "disabled"), retryable = false)
    }
}
