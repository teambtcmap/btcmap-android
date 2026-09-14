package org.btcmap.area

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.settings.authToken
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class AreaSaveErrorHandlingTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val app = ApplicationProvider.getApplicationContext<App>()

    @Test
    fun save_whenUserMissingFromDatabase_doesNotLeakUncaughtException() {
        preferencesRule.prefs.authToken = "test-token"

        val uncaught = AtomicReference<Throwable?>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(
                            R.id.fragmentContainerView,
                            AreaFragment().apply {
                                arguments = Bundle().apply { putString("area_id", "1") }
                            },
                            AREA_TAG,
                        )
                    }
                }
                instrumentation.waitForIdleSync()

                Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                    uncaught.set(throwable)
                }
                try {
                    scenario.onActivity { activity ->
                        val fragment = activity.supportFragmentManager
                            .findFragmentByTag(AREA_TAG) as AreaFragment
                        val toolbar = fragment.requireView().findViewById<Toolbar>(R.id.toolbar)
                        toolbar.menu.performIdentifierAction(R.id.save, 0)
                    }
                } finally {
                    Thread.setDefaultUncaughtExceptionHandler(previousHandler)
                }
            }
        } finally {
            app.mapStyleUriForTesting = null
        }

        Assert.assertNull(
            "Exception from the save handler's coroutine escaped to the uncaught handler",
            uncaught.get(),
        )
    }

    companion object {
        private const val AREA_TAG = "area"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
