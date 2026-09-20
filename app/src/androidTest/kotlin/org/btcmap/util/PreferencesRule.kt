package org.btcmap.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.btcmap.settings.Settings
import org.btcmap.settings.prefs as appPrefs
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class PreferencesRule : TestRule {

    val context: Context = ApplicationProvider.getApplicationContext()

    val prefs: Settings = appPrefs

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                clear()
                try {
                    base.evaluate()
                } finally {
                    clear()
                }
            }
        }
    }

    private fun clear() {
        // Drop the settings published by older app versions too, so a one-off
        // import does not leak values from a previous run into the test.
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
        // A test may deliberately leave a broken database driver installed, so
        // clearing the stored settings must not fail the test teardown. The
        // in-memory cache is still dropped by clearForTesting itself.
        runCatching { appPrefs.clearForTesting() }
    }
}
