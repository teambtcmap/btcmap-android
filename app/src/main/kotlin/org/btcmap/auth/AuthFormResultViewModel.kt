package org.btcmap.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
 */
internal sealed interface AuthFormResult {
    /** A submitted sign-in or sign-up form. */
    data class Credentials(
        val mode: AuthMode,
        val username: String,
        val password: String,
        val extras: Bundle,
    ) : AuthFormResult

    /** A submitted change-password form. */
    data class ChangePassword(
        val currentPassword: String,
        val newPassword: String,
    ) : AuthFormResult
}

/**
 * Holds the last submitted credential form until the host fragment consumes it.
 * Both the form (through [hostAuthFormResults]) and the host (through
 * [authFormResults]) reach the same instance.
 */
internal class AuthFormResultViewModel : ViewModel() {
    /** Set by a form, cleared by the host once it has read it. */
    var pending: AuthFormResult? = null
}

/** The holder on this fragment, which consumes the forms it showed. */
internal fun Fragment.authFormResults(): AuthFormResultViewModel =
    ViewModelProvider(this)[AuthFormResultViewModel::class.java]

/** The holder shared with the fragment that showed this form. */
internal fun Fragment.hostAuthFormResults(): AuthFormResultViewModel =
    ViewModelProvider(requireParentFragment())[AuthFormResultViewModel::class.java]
