package org.btcmap.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.btcmap.settings.prefs as appPrefs
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class PreferencesRule : TestRule {

    val context: Context = ApplicationProvider.getApplicationContext()

    val prefs: SharedPreferences = appPrefs

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                prefs.edit().clear().commit()
                try {
                    base.evaluate()
                } finally {
                    prefs.edit().clear().commit()
                }
            }
        }
    }
}
