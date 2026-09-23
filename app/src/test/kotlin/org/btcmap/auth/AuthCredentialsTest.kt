package org.btcmap.auth

import android.os.Bundle
import org.junit.Assert
import org.junit.Test

/**
 * The credential holders must never render their passwords. The compiler
 * generated data class `toString()` would, and these objects are exactly the
 * kind a future log line or crash report might print.
 */
class AuthCredentialsTest {

    @Test
    fun authCredentials_toString_redactsThePassword() {
        val credentials = AuthCredentials(
            mode = AuthMode.SignIn,
            username = "satoshi",
            password = "hunter2",
            extras = Bundle(),
        )

        val text = credentials.toString()

        Assert.assertFalse(text.contains("hunter2"))
        Assert.assertTrue(text.contains("satoshi"))
    }

    @Test
    fun changePasswordCredentials_toString_redactsBothPasswords() {
        val credentials = ChangePasswordCredentials(
            currentPassword = "old-secret",
            newPassword = "new-secret",
        )

        val text = credentials.toString()

        Assert.assertFalse(text.contains("old-secret"))
        Assert.assertFalse(text.contains("new-secret"))
    }
}
