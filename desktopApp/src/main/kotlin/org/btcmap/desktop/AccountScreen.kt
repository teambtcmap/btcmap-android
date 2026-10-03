package org.btcmap.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.btcmap.api.Api
import org.btcmap.auth.AuthError
import org.btcmap.auth.AuthOutcome
import org.btcmap.auth.AuthSession
import org.btcmap.auth.AuthValidation
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.settings.authorized

/** Test tags so the desktop tests can drive the account form. */
internal const val ACCOUNT_USERNAME_TAG = "account-username"
internal const val ACCOUNT_PASSWORD_TAG = "account-password"
internal const val ACCOUNT_CONFIRM_TAG = "account-confirm"
internal const val ACCOUNT_SUBMIT_TAG = "account-submit"
internal const val ACCOUNT_TOGGLE_TAG = "account-toggle"

private const val REQUIRED = "Required"

/**
 * The desktop's account page. It signs in or signs up with the same credentials
 * the app uses and stores the same session the rest of the app reads. When
 * signed in it shows the shared profile (saved places and areas, change username
 * and password, and log out).
 */
@Composable
internal fun DesktopAccountScreen(
    api: Api,
    db: Database,
    settings: Settings,
    onBack: () -> Unit,
) {
    var authorized by remember { mutableStateOf(settings.authorized) }
    var signUp by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenPage(title = "Account", onBack = onBack) {
        if (authorized) {
            DesktopProfile(
                api = api,
                db = db,
                settings = settings,
                onLoggedOut = { authorized = false },
            )
        } else {
            AuthForm(
                signUp = signUp,
                username = username,
                password = password,
                confirmation = confirmation,
                attempted = attempted,
                busy = busy,
                error = error,
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                onConfirmationChange = { confirmation = it },
                onToggleMode = {
                    signUp = !signUp
                    attempted = false
                    error = null
                },
                onSubmit = {
                    attempted = true
                    val errors = if (signUp) {
                        AuthValidation.signUp(username.trim(), password, confirmation)
                    } else {
                        AuthValidation.signIn(username.trim(), password)
                    }
                    if (errors.isEmpty()) {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val outcome = if (signUp) {
                                    AuthSession.signUp(
                                        api = api,
                                        db = db,
                                        prefs = settings,
                                        username = username,
                                        password = password,
                                        label = DESKTOP_TOKEN_LABEL,
                                    )
                                } else {
                                    AuthSession.signIn(
                                        api = api,
                                        db = db,
                                        prefs = settings,
                                        username = username,
                                        password = password,
                                        label = DESKTOP_TOKEN_LABEL,
                                    )
                                }
                                when (outcome) {
                                    is AuthOutcome.Authenticated -> {
                                        signUp = false
                                        attempted = false
                                        password = ""
                                        confirmation = ""
                                        authorized = true
                                    }

                                    is AuthOutcome.AccountCreated -> {
                                        // The account exists; only its local
                                        // session could not be established, so
                                        // fall back to the sign-in form.
                                        signUp = false
                                        attempted = false
                                        error = "Account created. Please sign in."
                                    }

                                    is AuthOutcome.Failed ->
                                        error = outcome.error.message ?: outcome.error.toString()
                                }
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
            )
        }
    }
}

/**
 * The sign-in / sign-up fields. The submit button stays disabled until the
 * required fields are filled; the per-field messages show once an attempt was
 * made (a too-short or mismatched password).
 */
@Composable
private fun AuthForm(
    signUp: Boolean,
    username: String,
    password: String,
    confirmation: String,
    attempted: Boolean,
    busy: Boolean,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
) {
    val errors = when {
        !attempted -> emptyList()
        signUp -> AuthValidation.signUp(username.trim(), password, confirmation)
        else -> AuthValidation.signIn(username.trim(), password)
    }
    val usernameError = errors.contains(AuthError.UsernameRequired)
    val passwordTooShort = errors.contains(AuthError.PasswordTooShort)
    val passwordError = passwordTooShort || errors.contains(AuthError.PasswordRequired)
    val confirmationError = errors.contains(AuthError.PasswordsDoNotMatch)

    val ready = username.isNotBlank() &&
        password.isNotBlank() &&
        (!signUp || confirmation.isNotBlank())

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(text = "Username") },
            singleLine = true,
            isError = usernameError,
            supportingText = if (usernameError) {
                { Text(REQUIRED) }
            } else {
                null
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ACCOUNT_USERNAME_TAG),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(text = "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError,
            supportingText = when {
                passwordTooShort ->
                    { { Text("At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters") } }

                passwordError -> {
                    { Text(REQUIRED) }
                }

                signUp -> {
                    { Text("At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters") }
                }

                else -> null
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ACCOUNT_PASSWORD_TAG),
        )
        if (signUp) {
            OutlinedTextField(
                value = confirmation,
                onValueChange = onConfirmationChange,
                label = { Text(text = "Confirm password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmationError,
                supportingText = if (confirmationError) {
                    { Text("Passwords do not match") }
                } else {
                    null
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ACCOUNT_CONFIRM_TAG),
            )
        }
        error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy && ready,
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ACCOUNT_SUBMIT_TAG),
        ) {
            Text(text = if (signUp) "Create account" else "Sign in")
        }
        TextButton(
            enabled = !busy,
            onClick = onToggleMode,
            modifier = Modifier.testTag(ACCOUNT_TOGGLE_TAG),
        ) {
            Text(text = if (signUp) "I already have an account" else "Create an account")
        }
        if (!signUp) {
            Text(
                text = "Accounts can also be created in the mobile app or on btcmap.org.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** The label this machine's session shows up under in the account's devices. */
private const val DESKTOP_TOKEN_LABEL = "BTC Map desktop"
