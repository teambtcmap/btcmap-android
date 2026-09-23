package org.btcmap.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.junit.Assert
import org.junit.Test

class AuthFormViewModelTest {

    @Test
    fun onCleared_dropsTypedPasswords() {
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val model = ViewModelProvider(owner)[AuthFormViewModel::class.java]
        model.currentPassword = "current"
        model.password = "secret"
        model.confirmation = "secret"

        owner.viewModelStore.clear()

        // The form is gone for good, so the typed passwords must not linger in
        // memory. A configuration change keeps the view model and does not clear
        // it, which is what lets the values survive a rotation.
        Assert.assertEquals("", model.currentPassword)
        Assert.assertEquals("", model.password)
        Assert.assertEquals("", model.confirmation)
    }
}
