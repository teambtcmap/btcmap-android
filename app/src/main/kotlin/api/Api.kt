package api

import area.AreaJson
import element.ElementJson
import event.EventJson
import reports.ReportJson
import java.time.ZonedDateTime

interface Api {

    suspend fun getElements(updatedSince: ZonedDateTime?, limit: Long): List<ElementJson>

    suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson>

    suspend fun getEvents(updatedSince: ZonedDateTime?, limit: Long): List<EventJson>

    suspend fun getReports(updatedSince: ZonedDateTime?, limit: Long): List<ReportJson>
}