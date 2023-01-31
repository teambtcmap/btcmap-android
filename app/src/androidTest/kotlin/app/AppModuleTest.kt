package app

import androidx.test.platform.app.InstrumentationRegistry
import area.AreaModel
import org.junit.Test
import org.koin.android.ext.android.get

class AppModuleTest {

    @Test
    fun dependencies() {
        val app =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApp

        app.get<AreaModel>()
    }
}