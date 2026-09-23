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

    /**
     * Drops any typed password once the form is gone for good (submitted or
     * dismissed), so it does not linger until the fragment instance is
     * collected. A configuration change keeps this view model, so [onCleared] is
     * not called and the values still survive a rotation.
     */
    override fun onCleared() {
        currentPassword = ""
        password = ""
        confirmation = ""
    }
}
