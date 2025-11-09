package fragment

import androidx.fragment.app.Fragment

fun Fragment.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}