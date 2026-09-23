package org.btcmap.util

import android.view.ViewParent
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Shows [error] on the [TextInputLayout] wrapping this field, or clears it when
 * [error] is null. Does nothing when the field is null or is not wrapped in a
 * [TextInputLayout].
 *
 * A [TextInputLayout] renders errors set on itself, not on its child edit text:
 * setting the error on the child falls back to the framework error popup and
 * leaves the layout's helper text in place. Use this to get the inline Material
 * error instead.
 */
fun TextInputEditText?.setFieldError(error: CharSequence?) {
    val layout = this?.findTextInputLayout() ?: return
    layout.error = error
}

private fun TextInputEditText.findTextInputLayout(): TextInputLayout? {
    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is TextInputLayout) return parent
        parent = parent.parent
    }
    return null
}
