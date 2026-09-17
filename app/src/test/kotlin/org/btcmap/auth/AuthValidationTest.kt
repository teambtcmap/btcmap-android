package org.btcmap.auth

import org.junit.Assert
import org.junit.Test

class AuthValidationTest {

    @Test
    fun signIn_acceptsFilledCredentials() {
        Assert.assertTrue(AuthValidation.signIn("satoshi", "short").isEmpty())
    }

    @Test
    fun signIn_requiresUsernameAndPassword() {
        val errors = AuthValidation.signIn("  ", "")

        Assert.assertEquals(
            listOf(AuthError.UsernameRequired, AuthError.PasswordRequired),
            errors,
        )
    }

    @Test
    fun signIn_doesNotEnforcePasswordLength() {
        // Accounts created before the minimum existed must still sign in.
        Assert.assertTrue(AuthValidation.signIn("satoshi", "123").isEmpty())
    }

    @Test
    fun signUp_requiresMatchingConfirmation() {
        val errors = AuthValidation.signUp(
            username = "satoshi",
            password = "password1",
            confirmation = "different",
        )

        Assert.assertEquals(listOf(AuthError.PasswordsDoNotMatch), errors)
    }

    @Test
    fun signUp_rejectsTooShortPassword() {
        val errors = AuthValidation.signUp(
            username = "satoshi",
            password = "short",
            confirmation = "short",
        )

        Assert.assertEquals(listOf(AuthError.PasswordTooShort), errors)
    }

    @Test
    fun signUp_acceptsMinimumLength() {
        val minimum = "a".repeat(AuthValidation.MIN_PASSWORD_LENGTH)

        Assert.assertTrue(AuthValidation.signUp("satoshi", minimum, minimum).isEmpty())
    }

    @Test
    fun signUp_countsCharactersNotUtf16Units() {
        // Four emoji are four characters but eight UTF-16 units, so a length
        // check on String.length would wrongly accept them.
        val password = "😀".repeat(4)

        val errors = AuthValidation.signUp("satoshi", password, password)

        Assert.assertEquals(listOf(AuthError.PasswordTooShort), errors)
    }

    @Test
    fun signUp_reportsEveryProblemAtOnce() {
        val errors = AuthValidation.signUp("", "", "mismatch")

        Assert.assertTrue(AuthError.UsernameRequired in errors)
        Assert.assertTrue(AuthError.PasswordRequired in errors)
        Assert.assertTrue(AuthError.PasswordsDoNotMatch in errors)
    }

    @Test
    fun changePassword_acceptsValidInput() {
        Assert.assertTrue(
            AuthValidation.changePassword("old-password", "new-password", "new-password").isEmpty()
        )
    }

    @Test
    fun changePassword_requiresCurrentPassword() {
        val errors = AuthValidation.changePassword("", "new-password", "new-password")

        Assert.assertEquals(listOf(AuthError.CurrentPasswordRequired), errors)
    }

    @Test
    fun changePassword_requiresNewPassword() {
        val errors = AuthValidation.changePassword("old-password", "", "")

        Assert.assertEquals(listOf(AuthError.PasswordRequired), errors)
    }

    @Test
    fun changePassword_rejectsTooShortNewPassword() {
        val errors = AuthValidation.changePassword("old-password", "short", "short")

        Assert.assertEquals(listOf(AuthError.PasswordTooShort), errors)
    }

    @Test
    fun changePassword_requiresMatchingConfirmation() {
        val errors =
            AuthValidation.changePassword("old-password", "new-password", "different")

        Assert.assertEquals(listOf(AuthError.PasswordsDoNotMatch), errors)
    }
}
