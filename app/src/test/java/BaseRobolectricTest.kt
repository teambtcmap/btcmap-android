import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.dagger.MainComponent

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * @author Igor Bubelov
 */

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, shadows = arrayOf(NetworkSecurityPolicyShadow::class))
abstract class BaseRobolectricTest {
    val dependencies: MainComponent? = null
    get() {
        if (field == null) {
            Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application)
            field = Injector.INSTANCE.mainComponent()
        }

        return field
    }
}