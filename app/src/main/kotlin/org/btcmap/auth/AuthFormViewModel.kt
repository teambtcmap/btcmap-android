package org.btcmap.auth

import androidx.lifecycle.ViewModel

/**
 * Holds the passwords typed into a credential form.
 *
 * A fragment's [ViewModel] is retained while the activity is recreated after a
 * configuration change, but it is not written to saved instance state. Keeping
 * the passwords here lets a form survive a rotation without persisting them for
 * a possible process death, unlike the dialog view hierarchy, which is saved.
 */
internal class AuthFormViewModel : ViewModel() {
    var currentPassword: String = ""
    var password: String = ""
    var confirmation: String = ""
}
