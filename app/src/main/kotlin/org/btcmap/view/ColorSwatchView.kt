package org.btcmap.view

import kotlin.math.roundToInt
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * A rounded color swatch with a subtle outline. The outline keeps light and
 * dark colors visible against any surface, so the color itself is never shown
 * by tinting text.
 */
class ColorSwatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val swatch = GradientDrawable().apply {
        cornerRadius = resources.displayMetrics.density * CORNER_RADIUS_DP
        setStroke(
            (resources.displayMetrics.density * BORDER_WIDTH_DP).roundToInt(),
            MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, 0),
        )
    }

    init {
        background = swatch
    }

    fun setColor(color: Int) {
        swatch.setColor(color)
    }

    companion object {
        private const val CORNER_RADIUS_DP = 4f
        private const val BORDER_WIDTH_DP = 1f
    }
}
