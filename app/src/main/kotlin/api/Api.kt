package api

import area.AreaJson
import element.ElementJson
import event.EventJson
import reports.ReportJson
import user.UserJson
import java.time.ZonedDateTime

interface Api {

    suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson>

    suspend fun getElements(updatedSince: ZonedDateTime?, limit: Long): List<ElementJson>

    suspend fun getEvents(updatedSince: ZonedDateTime?, limit: Long): List<EventJson>

    suspend fun getReports(updatedSince: ZonedDateTime?, limit: Long): List<ReportJson>

    suspend fun getUsers(updatedSince: ZonedDateTime?, limit: Long): List<UserJson>
}