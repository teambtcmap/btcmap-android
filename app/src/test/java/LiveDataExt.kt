import android.arch.lifecycle.LiveData
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Igor Bubelov
 */

fun <T> LiveData<T>.blockingObserve(): T {
    var value: T? = null
    val latch = CountDownLatch(1)

    observeForever({
        value = it
        latch.countDown()
    })

    latch.await(10, TimeUnit.SECONDS)
    @Suppress("UNCHECKED_CAST")
    return value as T
}