package api

import area.AreaJson
import java.time.ZonedDateTime

interface Api {

    suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson>
}