package org.btcmap.auth

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.btcmap.ui.AuthField
import org.btcmap.ui.AuthFormComposeView
import org.btcmap.util.iconTypeface

/**
 * Shared scaffolding for the credential dialogs: the positive/negative buttons,
 * submitting from the keyboard, and showing [AuthError]s on the matching fields.
 *
 * The form content is a Compose `AuthFormComposeView` set into a
 * `MaterialAlertDialog`; the typed values live here and in the subclass's
 * retained view model, so a rotation recreates the form with the same values
 * (and, for passwords, without persisting them). A subclass describes its
 * fields, validates them and reports the result.
 */
internal abstract class AuthFormDialogFragment : DialogFragment() {

    @get:StringRes
    protected abstract val titleRes: Int

    @get:StringRes
    protected abstract val positiveRes: Int

    /** The fields to render, with their labels, retained values and helpers. */
    protected abstract fun fields(): List<AuthField>

    /** Validates the current values, returning every problem found. */
    protected abstract fun validate(values: Map<String, String>): List<AuthError>

    /** Reports the collected input; runs only when [validate] returns no errors. */
    protected abstract fun onSubmit(values: Map<String, String>)

    /** Persists the typed values so they survive a configuration change. */
    protected open fun onValuesChanged(values: Map<String, String>) = Unit

    /** The field key a given error belongs to. */
    protected abstract fun fieldKeyFor(error: AuthError): String

    /** The message shown for a given error. */
    protected abstract fun errorMessage(error: AuthError): String

    private lateinit var formView: AuthFormComposeView
    private val values = mutableMapOf<String, String>()

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        values.clear()
        fields().forEach { values[it.key] = it.value }

        val iconFont = iconTypeface
        formView = AuthFormComposeView(requireContext()).apply {
            setViewTreeLifecycleOwner(this@AuthFormDialogFragment)
            setViewTreeSavedStateRegistryOwner(this@AuthFormDialogFragment)
            setViewTreeViewModelStoreOwner(this@AuthFormDialogFragment)
            iconTypeface = iconFont
            onValueChange = { key, value ->
                values[key] = value
                onValuesChanged(values)
                render(emptyList())
            }
            onDone = { submit() }
        }
        render(emptyList())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(formView)
            .setPositiveButton(positiveRes, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { submit() }
        }

        return dialog
    }

    private fun render(errors: List<AuthError>) {
        val errorByKey = errors.associate { fieldKeyFor(it) to errorMessage(it) }
        formView.fields = fields().map { field ->
            field.copy(
                value = values[field.key] ?: field.value,
                error = errorByKey[field.key],
            )
        }
    }

    private fun submit() {
        val errors = validate(values)
        if (errors.isEmpty()) {
            onSubmit(values)
            dismiss()
            return
        }
        render(errors)
    }
}
