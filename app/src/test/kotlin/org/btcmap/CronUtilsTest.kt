package org.btcmap

import com.cronutils.model.Cron
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class CronUtilsTest {

    @Test
    fun calculateNextRun_quartzCron_returnsNextExecution() {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(com.cronutils.model.CronType.QUARTZ)
        val parser = CronParser(cronDefinition)

        val cron: Cron = parser.parse("0 0 19 ? * 6L")
        val executionTime = ExecutionTime.forCron(cron)

        val now = ZonedDateTime.now()
        val nextRun = executionTime.nextExecution(now)

        Assert.assertNotNull(nextRun)
        Assert.assertTrue(nextRun.isPresent)
        Assert.assertTrue(nextRun.get().isAfter(now))
    }

    @Test
    fun parseCron_validExpression_parsesSuccessfully() {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(com.cronutils.model.CronType.QUARTZ)
        val parser = CronParser(cronDefinition)

        val cron = parser.parse("0 23 * ? * 1-5 *")

        Assert.assertNotNull(cron)
    }
}