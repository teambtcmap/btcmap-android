package org.btcmap.util

import org.junit.Rule

/**
 * Base class for tests that drive the app.
 *
 * Gives each test a mock API, an in-memory database, cleared settings, the
 * app-scoped sync disabled and the offline map style, and exposes them under
 * the names the tests use. Extend this instead of declaring the individual
 * rules, and pass `sync = true` when the sync itself is under test.
 */
abstract class AppTestCase(
    sync: Boolean = false,
) {
    @get:Rule
    val rules = AppTestRule(sync = sync)

    protected val apiRule get() = rules.apiRule

    protected val databaseRule get() = rules.databaseRule

    protected val preferencesRule get() = rules.preferencesRule
}
