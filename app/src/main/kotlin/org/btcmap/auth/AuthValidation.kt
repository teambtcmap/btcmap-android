package org.btcmap.auth

/**
 * Client-side checks for the sign-in, sign-up and change-password forms.
 *
 * The API's REST endpoints (`POST /v4/users`, `PUT /v4/users/me/password`) only
 * require non-empty passwords; the 12-character minimum in
 * `service::auth::MIN_PASSWORD_LENGTH` is enforced by the RPC handlers instead.
 * This app applies its own stricter minimum to discourage weak passwords. It is
 * checked only when a password is set (sign-up and change-password), so accounts
 * created before the rule existed can still sign in.
 */
internal enum class AuthError {
    UsernameRequired,
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
        if (password != confirmation) add(AuthError.PasswordsDoNotMatch)
    }

    fun changePassword(
        current: String,
        new: String,
        confirmation: String,
    ): List<ChangePasswordError> = buildList {
        if (current.isEmpty()) add(ChangePasswordError.CurrentRequired)
        when {
            new.isEmpty() -> add(ChangePasswordError.NewRequired)
            new.characterCount() < MIN_PASSWORD_LENGTH -> add(ChangePasswordError.NewTooShort)
        }
        if (new != confirmation) add(ChangePasswordError.ConfirmationMismatch)
    }

    private fun String.characterCount(): Int = codePointCount(0, length)
}

internal enum class ChangePasswordError {
    CurrentRequired,
    NewRequired,
    NewTooShort,
    ConfirmationMismatch,
}
