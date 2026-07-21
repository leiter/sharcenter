package cut.the.crap.data.rest.campaign

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw JSON for the action campaign as served by the campaign site:
 *
 *     https://cutthecrap.link/api/abu-safiya
 *
 * The endpoint mirrors the site's own country registry (`routes/abusafiya.py`) plus the
 * hand-written social posts from `social/abu_safiya_x_posts.md`, so the app never has to
 * build campaign URLs or hold campaign text of its own. The client is configured with
 * `ignoreUnknownKeys = true`, so fields added server-side are simply ignored.
 */
@Serializable
internal data class CampaignDto(
    @SerialName("campaign")
    val campaign: String = "",

    @SerialName("version")
    val version: Int = 1,

    /** Geolocating entry point that redirects to the visitor's own country page. */
    @SerialName("locateUrl")
    val locateUrl: String? = null,

    @SerialName("countries")
    val countries: List<CampaignCountryDto> = emptyList()
)

@Serializable
internal data class CampaignCountryDto(
    /** Routing code, lower-case (e.g. "it", "uk"). Not always the ISO code — see the site. */
    @SerialName("code")
    val code: String,

    @SerialName("name")
    val name: String = "",

    @SerialName("flag")
    val flag: String = "",

    /** Language used when the country page is opened without an explicit `?lang=`. */
    @SerialName("defaultLang")
    val defaultLang: String = "",

    @SerialName("langs")
    val langs: List<String> = emptyList(),

    @SerialName("url")
    val url: String = "",

    /** True when the country also offers the write-to-your-MP action. */
    @SerialName("parliament")
    val parliament: Boolean = false,

    @SerialName("posts")
    val posts: List<CampaignPostDto> = emptyList()
)

@Serializable
internal data class CampaignPostDto(
    /** Variant label as written in the source markdown, e.g. "IT-2 (breve)". */
    @SerialName("id")
    val id: String = "",

    @SerialName("lang")
    val lang: String = "",

    /** The complete post, campaign link and hashtags included — ready to publish. */
    @SerialName("text")
    val text: String = ""
)

// ---------------------------------------------------------------------------
// Domain models exposed to the rest of the app
// ---------------------------------------------------------------------------

/**
 * One ready-to-publish post for a country in one language.
 *
 * @property id Variant label from the campaign's own copy deck (e.g. "IT-1", "CA-2 (short)").
 * @property text The full post — prose, campaign link and hashtags. Never templated by the app.
 */
data class CampaignPost(
    val id: String,
    val language: String,
    val text: String
)

/**
 * One country of the campaign, with the posts written for it.
 *
 * @property url The country's campaign page, e.g. "https://cutthecrap.link/it/abu-safiya".
 * @property hasParliamentAction Whether the site also offers the write-to-your-MP action there.
 */
data class CampaignCountry(
    val countryCode: String,
    val countryName: String,
    val flag: String,
    val defaultLanguage: String,
    val languages: List<String>,
    val url: String,
    val hasParliamentAction: Boolean,
    val posts: List<CampaignPost>
) {
    /** Posts grouped by language, each group keeping the campaign's own variant order. */
    val postsByLanguage: Map<String, List<CampaignPost>> = posts.groupBy { it.language }

    /** True when this country can contribute drafts — drives selectability in the composer. */
    val hasPosts: Boolean get() = posts.isNotEmpty()

    /** Drafts produced by selecting this country: one per language it has posts in. */
    val postCount: Int get() = postsByLanguage.size

    /** Most variants any one language offers here — the variant picker's upper bound. */
    val variantCount: Int get() = postsByLanguage.values.maxOfOrNull { it.size } ?: 0

    /**
     * The [variantIndex]-th post of each language, clamped per language so a country with
     * fewer variants than the selected index falls back to its last one.
     */
    fun postsForVariant(variantIndex: Int): List<CampaignPost> =
        postsByLanguage.values.map { it[variantIndex.coerceIn(0, it.lastIndex)] }
}

/**
 * The campaign as a whole.
 *
 * @property countries In the site's own registry order, which puts the primary countries first.
 */
data class Campaign(
    val name: String,
    val version: Int,
    val locateUrl: String?,
    val countries: List<CampaignCountry>
) {
    /** Countries that actually have posts — the only ones worth offering in the composer. */
    val countriesWithPosts: List<CampaignCountry> get() = countries.filter { it.hasPosts }
}
