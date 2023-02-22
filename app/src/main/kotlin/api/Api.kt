package api

import area.AreaJson
import element.ElementJson
import java.time.ZonedDateTime

interface Api {

    suspend fun getElements(updatedSince: ZonedDateTime?, limit: Long): List<ElementJson>

    suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson>
}