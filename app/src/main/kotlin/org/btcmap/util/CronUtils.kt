package org.btcmap.util

import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import java.time.ZoneId
import java.time.ZonedDateTime

object CronUtils {
    private val parser = CronParser(
        CronDefinitionBuilder.instanceDefinitionFor(com.cronutils.model.CronType.QUARTZ)
    )

    fun nextExecutions(cronSchedule: String, count: Int, from: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): List<ZonedDateTime> {
        val cron = parser.parse(cronSchedule)
        val executionTime = ExecutionTime.forCron(cron)
        val result = mutableListOf<ZonedDateTime>()
        var current = from
        repeat(count) {
            val next = executionTime.nextExecution(current)
            if (next.isPresent) {
                result.add(next.get())
                current = next.get().plusSeconds(1)
            }
        }
        return result
    }
}