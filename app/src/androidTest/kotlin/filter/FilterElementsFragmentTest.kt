package filter

import androidx.fragment.app.testing.launchFragmentInContainer
import org.junit.Test

class FilterElementsFragmentTest {

    @Test
    fun launch() {
        launchFragmentInContainer<FilterElementsFragment>(
            themeResId = com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight,
        ).use { scenario ->
            scenario.onFragment {
                assert(it.view != null)
            }
        }
    }
}