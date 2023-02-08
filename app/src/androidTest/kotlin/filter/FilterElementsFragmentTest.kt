package filter

import androidx.fragment.app.testing.launchFragmentInContainer
import org.btcmap.R
import org.junit.Test

class FilterElementsFragmentTest {

    @Test
    fun launch() {
        launchFragmentInContainer<FilterElementsFragment>(
            themeResId = R.style.Theme_Material3_DynamicColors_DayNight,
        ).use { scenario ->
            scenario.onFragment {
                assert(it.view != null)
            }
        }
    }
}