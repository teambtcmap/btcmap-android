package app

import androidx.test.platform.app.InstrumentationRegistry
import coil.decode.SvgDecoder
import coil.imageLoader
import org.junit.Test

class AppTest {

    @Test
    fun imageLoader() {
        val app =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App
        val loader = app.imageLoader
        assert(loader.components.decoderFactories.contains(SvgDecoder.Factory()))
    }
}