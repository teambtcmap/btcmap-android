import android.app.Application
import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * @author Igor Bubelov
 */

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
abstract class BaseRobolectricTest {
    @JvmField @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    init {
        TestInjector.init(RuntimeEnvironment.application)
    }
}