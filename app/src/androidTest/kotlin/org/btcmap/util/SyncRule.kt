package org.btcmap.util

import androidx.test.core.app.ApplicationProvider
import org.btcmap.App
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Controls the app-scoped sync for one test.
 *
 * The sync is disabled by default: a no-op [TestSyncController] is installed so
 * the background full sync (which seeds the real bundled snapshots and hits the
 * API) cannot disturb a test that only uses the database or the mocked network.
 * Pass `enabled = true` from a test that exercises the sync itself.
 */
class SyncRule(
    private val enabled: Boolean = false,
) : TestRule {

    private val app = ApplicationProvider.getApplicationContext<App>()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (!enabled) {
                    app.syncControllerForTesting = TestSyncController { app.sync }
                }
                try {
                    base.evaluate()
                } finally {
                    app.syncControllerForTesting = null
                }
            }
        }
    }
}
