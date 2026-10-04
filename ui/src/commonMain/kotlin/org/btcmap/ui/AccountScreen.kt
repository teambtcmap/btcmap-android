package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
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

/** Test tags so a test can drive the account form. */
const val ACCOUNT_USERNAME_TAG = "account-username"
const val ACCOUNT_PASSWORD_TAG = "account-password"
const val ACCOUNT_CONFIRM_TAG = "account-confirm"
const val ACCOUNT_SUBMIT_TAG = "account-submit"
const val ACCOUNT_TOGGLE_TAG = "account-toggle"

/** Field keys for the shared [AuthFormContent]. */
private const val ACCOUNT_USERNAME_KEY = "username"
private const val ACCOUNT_PASSWORD_KEY = "password"
private const val ACCOUNT_CONFIRM_KEY = "confirmation"

/** The account form's strings, so the screen stays resource-free. */
data class AccountLabels(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val required: String,
    val passwordTooShort: (minLength: Int) -> String,
    val passwordsDoNotMatch: String,
    val signIn: String,
    val createAccount: String,
    val alreadyHaveAccount: String,
    val createAnAccount: String,
    val accountCreated: String,
)

/**
 * Signs in or signs up with the shared [AuthSession], and shows the shared
 * profile once signed in.
 *
 * The signed-in content is the host's [profile] slot, because Android and the
 * desktop render different profile chrome. [tokenLabel] names this device's
 * session in the account's token list. The host supplies its own top bar.
 */
@Composable
fun AccountScreen(
    api: Api,
    db: Database,
    settings: Settings,
    tokenLabel: String,
    labels: AccountLabels,
    profile: @Composable (onLoggedOut: () -> Unit) -> Unit,
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

    if (authorized) {
        profile { authorized = false }
        return
    }

    AccountAuthForm(
        signUp = signUp,
        username = username,
        password = password,
        confirmation = confirmation,
        attempted = attempted,
        busy = busy,
        error = error,
        labels = labels,
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
                                label = tokenLabel,
                            )
                        } else {
                            AuthSession.signIn(
                                api = api,
                                db = db,
                                prefs = settings,
                                username = username,
                                password = password,
                                label = tokenLabel,
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
                                // The account exists; only its local session
                                // could not be established, so fall back to the
                                // sign-in form.
                                signUp = false
                                attempted = false
                                error = labels.accountCreated
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

/**
 * The sign-in / sign-up fields. The submit button stays disabled until the
 * required fields are filled; the per-field messages show once an attempt was
 * made (a too-short or mismatched password).
 */
@Composable
private fun AccountAuthForm(
    signUp: Boolean,
    username: String,
    password: String,
    confirmation: String,
    attempted: Boolean,
    busy: Boolean,
    error: String?,
    labels: AccountLabels,
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

    val fields = listOfNotNull(
        AuthField(
            key = ACCOUNT_USERNAME_KEY,
            label = labels.username,
            value = username,
            isPassword = false,
            error = if (usernameError) labels.required else null,
            imeAction = AuthImeAction.Next,
            contentType = if (signUp) ContentType.NewUsername else ContentType.Username,
            testTag = ACCOUNT_USERNAME_TAG,
        ),
        AuthField(
            key = ACCOUNT_PASSWORD_KEY,
            label = labels.password,
            value = password,
            isPassword = true,
            error = when {
                passwordTooShort -> labels.passwordTooShort(AuthValidation.MIN_PASSWORD_LENGTH)
                passwordError -> labels.required
                else -> null
            },
            helper = if (signUp) {
                labels.passwordTooShort(AuthValidation.MIN_PASSWORD_LENGTH)
            } else {
                null
            },
            imeAction = if (signUp) AuthImeAction.Next else AuthImeAction.Done,
            contentType = if (signUp) ContentType.NewPassword else ContentType.Password,
            testTag = ACCOUNT_PASSWORD_TAG,
        ),
        if (signUp) {
            AuthField(
                key = ACCOUNT_CONFIRM_KEY,
                label = labels.confirmPassword,
                value = confirmation,
                isPassword = true,
                error = if (confirmationError) labels.passwordsDoNotMatch else null,
                imeAction = AuthImeAction.Done,
                contentType = ContentType.NewPassword,
                testTag = ACCOUNT_CONFIRM_TAG,
            )
        } else {
            null
        },
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            AuthFormContent(
                fields = fields,
                onValueChange = { key, value ->
                    when (key) {
                        ACCOUNT_USERNAME_KEY -> onUsernameChange(value)
                        ACCOUNT_PASSWORD_KEY -> onPasswordChange(value)
                        ACCOUNT_CONFIRM_KEY -> onConfirmationChange(value)
                    }
                },
                onDone = onSubmit,
                contentPadding = PaddingValues(0.dp),
            )
            error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Button(
                enabled = !busy && ready,
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ACCOUNT_SUBMIT_TAG),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(text = if (signUp) labels.createAccount else labels.signIn)
                }
            }
            TextButton(
                enabled = !busy,
                onClick = onToggleMode,
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier.testTag(ACCOUNT_TOGGLE_TAG),
            ) {
                Text(text = if (signUp) labels.alreadyHaveAccount else labels.createAnAccount)
            }
        }
    }
}
