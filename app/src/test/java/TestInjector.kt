import android.content.Context

/**
 * @author Igor Bubelov
 */

object TestInjector {
    lateinit var testComponent: TestComponent
        private set

    fun init(context: Context) {
        testComponent = DaggerTestComponent.builder().context(context).build()
    }
}