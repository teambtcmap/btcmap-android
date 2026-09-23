package org.btcmap.auth

/**
 * Client-side checks for the sign-in, sign-up and change-password forms.
 *
 * The REST endpoints this app uses (`POST /v4/users`,
 * `PUT /v4/users/me/password`) perform no password-length validation, so this
 * app applies its own minimum to discourage weak passwords. It is checked only
 * when a password is set (sign-up and change-password), so accounts created
 * before the rule existed can still sign in.
 */
internal enum class AuthError {
    UsernameRequired,
    CurrentPasswordRequired,
    PasswordRequired,
    PasswordTooShort,
    PasswordsDoNotMatch,
}

internal object AuthValidation {
    /** Minimum number of characters accepted when setting a password. */
    const val MIN_PASSWORD_LENGTH = 8

    fun signIn(username: String, password: String): List<AuthError> = buildList {
        if (username.isBlank()) add(AuthError.UsernameRequired)
        if (password.isEmpty()) add(AuthError.PasswordRequired)
    }

    fun signUp(username: String, password: String, confirmation: String): List<AuthError> = buildList {
        if (username.isBlank()) add(AuthError.UsernameRequired)
        when {
            password.isEmpty() -> add(AuthError.PasswordRequired)
            password.characterCount() < MIN_PASSWORD_LENGTH -> add(AuthError.PasswordTooShort)
        }
        // A missing password is reported as [PasswordRequired]; there is nothing
        // to compare against, so a filled confirmation must not also show a
        // match error on the same field.
        if (password.isNotEmpty() && password != confirmation) add(AuthError.PasswordsDoNotMatch)
    }

    fun changePassword(
        current: String,
        new: String,
        confirmation: String,
    ): List<AuthError> = buildList {
        if (current.isEmpty()) add(AuthError.CurrentPasswordRequired)
        when {
            new.isEmpty() -> add(AuthError.PasswordRequired)
            new.characterCount() < MIN_PASSWORD_LENGTH -> add(AuthError.PasswordTooShort)
        }
        // A missing new password is reported as [PasswordRequired]; there is
        // nothing to compare against, so a filled confirmation must not also
        // show a match error on the same field.
        if (new.isNotEmpty() && new != confirmation) add(AuthError.PasswordsDoNotMatch)
    }

    private fun String.characterCount(): Int = codePointCount(0, length)
}
