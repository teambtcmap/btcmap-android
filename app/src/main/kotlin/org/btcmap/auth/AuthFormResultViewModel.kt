package org.btcmap.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** A submitted sign-in or sign-up form. */
internal data class AuthCredentials(
    val mode: AuthMode,
    val username: String,
    val password: String,
    val extras: Bundle,
) {
    // The generated data class toString() would print the password, which this
    // module otherwise never persists or logs. Keep it out of any string that
    // could reach a log or a crash report. The extras are left out too: they do
    // not identify this object's purpose and may hold arbitrary caller payload.
    override fun toString(): String =
        "AuthCredentials(mode=$mode, username=$username, password=***)"
}

/** A submitted change-password form. */
internal data class ChangePasswordCredentials(
    val currentPassword: String,
    val newPassword: String,
) {
    // See AuthCredentials.toString(): never render the passwords.
    override fun toString(): String =
        "ChangePasswordCredentials(currentPassword=***, newPassword=***)"
}

/**
 * A credential form's submitted values, waiting to be consumed by the fragment
 * that showed the form.
 *
 * The values are held in a host-scoped [ViewModel], not in the Fragment Result
 * API's bundle. `FragmentManager` writes a pending result bundle to saved
 * instance state when no started listener has consumed it yet (for example
 * while the host is stopped, or when the host never registered a listener),
 * which would persist the plaintext password. A [ViewModel] is retained across a
 * configuration change but is never saved, so the password survives a rotation
 * without being written to disk. The result bundle itself carries only a signal
 * that a form was submitted.
 *
 * The sign-in/sign-up and change-password forms have separate slots, so a
 * pending value from one flow can never be overwritten by the other.
 */
internal class AuthFormResultViewModel : ViewModel() {
    /** Set by an auth form, cleared by the host once it has read it. */
    var credentials: AuthCredentials? = null

    /** Set by a change-password form, cleared by the host once it has read it. */
    var changePassword: ChangePasswordCredentials? = null
}

/** The holder on this fragment, which consumes the forms it showed. */
internal fun Fragment.authFormResults(): AuthFormResultViewModel =
    ViewModelProvider(this)[AuthFormResultViewModel::class.java]

/** The holder shared with the fragment that showed this form. */
internal fun Fragment.hostAuthFormResults(): AuthFormResultViewModel =
    ViewModelProvider(requireParentFragment())[AuthFormResultViewModel::class.java]
