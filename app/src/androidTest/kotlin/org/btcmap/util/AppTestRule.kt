package org.btcmap.util

import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * The service rules shared by tests that drive the app: a mock API, an
 * in-memory database, cleared settings, the app-scoped sync disabled and the
 * offline map style.
 *
 * Prefer extending [AppTestCase] over declaring this by hand. Pass `sync = true`
 * from a test that needs the real app-scoped sync.
 */
class AppTestRule(
    sync: Boolean = false,
) : TestRule {

    val apiRule = ApiRule()

    val databaseRule = DatabaseRule()

    val preferencesRule = PreferencesRule()

    private val chain = RuleChain
        .outerRule(apiRule)
        .around(databaseRule)
        .around(preferencesRule)
        .around(SyncRule(enabled = sync))
        .around(MapStyleRule())

    override fun apply(base: Statement, description: Description): Statement =
        chain.apply(base, description)
}
