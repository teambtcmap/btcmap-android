package org.btcmap.util

import androidx.test.core.app.ApplicationProvider
import org.btcmap.App
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** The offline map style bundled for tests. */
const val OFFLINE_MAP_STYLE_URI = "asset://map-styles/test/style.json"

/**
 * Points the map at the bundled offline test style, so a test that shows the
 * map never reaches the network for a style.
 */
class MapStyleRule : TestRule {

    private val app = ApplicationProvider.getApplicationContext<App>()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                app.mapStyleUriForTesting = OFFLINE_MAP_STYLE_URI
                try {
                    base.evaluate()
                } finally {
                    app.mapStyleUriForTesting = null
                }
            }
        }
    }
}
