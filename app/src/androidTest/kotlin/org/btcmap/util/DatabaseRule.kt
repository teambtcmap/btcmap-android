package org.btcmap.util

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import org.btcmap.App
import org.btcmap.db.Database
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DatabaseRule : TestRule {

    private val app = ApplicationProvider.getApplicationContext<App>()

    val db = Database(AndroidSQLiteDriver(), ":memory:")

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                app.dbForTesting = db
                try {
                    base.evaluate()
                } finally {
                    app.dbForTesting = null
                }
            }
        }
    }
}
