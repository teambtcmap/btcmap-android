/**
 * @author Igor Bubelov
 */

import com.bubelov.coins.dagger.Injector

import org.junit.Before
import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Ignore
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, shadows = arrayOf(NetworkSecurityPolicyShadow::class))
open class BaseRobolectricTest {
    protected val dependencies = lazy { Injector.INSTANCE.mainComponent() }

    @Before
    open fun setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application)
    }
}