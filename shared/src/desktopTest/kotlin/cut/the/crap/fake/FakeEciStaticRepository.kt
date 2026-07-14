package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.eci.EciStatistics
import cut.the.crap.data.rest.eci.EciStatisticsRepository

class FakeEciStaticRepository : EciStatisticsRepository {
    override suspend fun getStatistics(pageUrl: String): Result<EciStatistics> {
        TODO("Not yet implemented")
    }

    override suspend fun getStatistics(year: Int, number: String): Result<EciStatistics> {
        TODO("Not yet implemented")
    }
}