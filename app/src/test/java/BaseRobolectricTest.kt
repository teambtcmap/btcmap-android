import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * @author Igor Bubelov
 */

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "app/src/main/AndroidManifest.xml", sdk = intArrayOf(23))
abstract class BaseRobolectricTest {
    init {
        TestInjector.init(RuntimeEnvironment.application)
    }
}