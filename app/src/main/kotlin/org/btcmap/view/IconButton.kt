package org.btcmap.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import org.btcmap.R
import org.btcmap.settings.buttonBackgroundColor
import org.btcmap.settings.buttonBorderColor
import org.btcmap.settings.buttonIconColor
import org.btcmap.settings.prefs

class IconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    var iconResId = R.drawable.icon_store

    var iconColor = 0

    var icon: Bitmap? = null
        private set

    private var spinAnimator: ObjectAnimator? = null
    private var fadeAnimator: ObjectAnimator? = null

    // The last visibility requested while a fade was already running. Chaining
    // through it instead of cancelling keeps both fades complete, so a sync that
    // finishes in a few milliseconds still fades in fully before fading out.
    private var pendingVisible: Boolean? = null

    // The alpha the button is meant to have while shown. Read from the layout so
    // the dimmed sync button fades to its configured opacity.
    private var shownAlpha = alpha

    var spinning = false
        set(value) {
            field = value
            updateSpin()
        }

    init {
        attrs?.let { parseAttributes(it) }
    }

    fun backgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    fun borderColor(color: Int) {
        borderPaint.color = color
        invalidate()
    }

    fun icon(resId: Int, color: Int) {
        icon = generateIcon(resId, color)
        invalidate()
    }

    fun iconColor(color: Int) {
        iconColor = color
        icon(iconResId, iconColor)
    }

    /**
     * Shows or hides the button with a fade. Requests made while a fade is
     * running are queued and played after the current one finishes, so a fade-in
     * is never cut short by a fade-out that arrives right after it.
     */
    fun setVisibleAnimated(visible: Boolean) {
        pendingVisible = visible
        startPendingVisibilityAnimation()
    }

    private fun startPendingVisibilityAnimation() {
        if (fadeAnimator != null) return

        val target = pendingVisible ?: return
        pendingVisible = null

        if (target && isVisible && alpha == shownAlpha) return
        if (!target && visibility != VISIBLE) return

        if (visibility != VISIBLE) {
            alpha = 0f
            visibility = VISIBLE
        }

        val animator = ObjectAnimator.ofFloat(
            this,
            View.ALPHA,
            alpha,
            if (target) shownAlpha else 0f,
        )
        animator.duration = FADE_DURATION_MS
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (fadeAnimator !== animation) return
                fadeAnimator = null
                if (!target) visibility = GONE
                startPendingVisibilityAnimation()
            }
        })

        // Assign before starting: an animation with a zero duration (for
        // example when animator scale is disabled) can end synchronously.
        fadeAnimator = animator
        animator.start()
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.IconButton,
            0,
            0
        )

        try {
            backgroundColor(
                typedArray.getColor(
                    R.styleable.IconButton_backgroundColor,
                    prefs.buttonBackgroundColor(context),
                )
            )

            borderColor(
                typedArray.getColor(
                    R.styleable.IconButton_borderColor,
                    prefs.buttonBorderColor(context),
                )
            )

            borderPaint.strokeWidth = typedArray.getDimension(
                R.styleable.IconButton_borderWidth,
                resources.displayMetrics.density * 2,
            )

            iconResId = typedArray.getResourceId(
                R.styleable.IconButton_iconSrc,
                R.drawable.icon_store,
            )

            iconColor = typedArray.getColor(
                R.styleable.IconButton_iconColor,
                prefs.buttonIconColor(context),
            )

            icon(iconResId, iconColor)

            if (typedArray.getBoolean(R.styleable.IconButton_selected, false)) {
                isSelected = true
            }

            spinning = typedArray.getBoolean(R.styleable.IconButton_spin, false)
        } finally {
            typedArray.recycle()
        }
    }

    private fun generateIcon(iconResId: Int, color: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, iconResId) ?: return null
        DrawableCompat.setTint(drawable, color)
        val drawableSize = (resources.displayMetrics.density * 22).toInt()
        return drawable.toBitmap(width = drawableSize, height = drawableSize)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSpin()
    }

    override fun onDetachedFromWindow() {
        stopSpin()
        stopFade()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateSpin()
    }

    private fun updateSpin() {
        if (spinning && isShown) startSpin() else stopSpin()
    }

    private fun startSpin() {
        if (spinAnimator != null) return
        spinAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, -360f).apply {
            duration = SPIN_DURATION_MS
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopSpin() {
        spinAnimator?.cancel()
        spinAnimator = null
        rotation = 0f
    }

    private fun stopFade() {
        fadeAnimator?.apply {
            // Drop the listener first so cancelling does not commit a
            // half-finished fade or kick off a queued one.
            removeAllListeners()
            cancel()
        }
        fadeAnimator = null
        pendingVisible = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        if (isSelected) {
            canvas.drawCircle(centerX, centerY, width / 2.0f - borderPaint.strokeWidth / 2, backgroundPaint)

            canvas.drawCircle(
                centerX,
                centerY,
                width / 2.0f - borderPaint.strokeWidth / 2,
                borderPaint,
            )
        } else {
            canvas.drawCircle(centerX, centerY, width / 2.0f, backgroundPaint)
        }

        icon?.let { icon ->
            canvas.drawBitmap(
                icon,
                width / 2f - icon.width / 2f,
                height / 2f - icon.height / 2f,
                null,
            )
        }
    }

    companion object {
        private const val FADE_DURATION_MS = 200L
        private const val SPIN_DURATION_MS = 1000L
    }
}