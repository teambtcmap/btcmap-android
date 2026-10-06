package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.btcmap.api.Api
import org.btcmap.auth.AuthOutcome
import org.btcmap.auth.AuthSession
import org.btcmap.auth.AuthValidation
import org.btcmap.db.Database
import org.btcmap.settings.Settings

/**
 * The sign-in / create-account dialog the shared root shows when a screen needs
 * a session. It reuses the shared form and [AuthSession], so Android and the
 * desktop sign in the same way; on success the caller dismisses it and re-reads
 * the session.
 *
 * The account chooser the old dialog fragments showed is folded into the form's
 * sign-in / create-account toggle.
 */
@Composable
internal fun AuthDialog(
    api: Api,
    db: Database,
    settings: Settings,
    tokenLabel: String,
    labels: AccountLabels,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    var signUp by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(
                    text = if (signUp) labels.createAnAccount else labels.signIn,
                    style = MaterialTheme.typography.headlineSmall,
                )

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
                                        is AuthOutcome.Authenticated -> onAuthenticated()

                                        is AuthOutcome.AccountCreated -> {
                                            // The account exists; only its session
                                            // could not be established, so fall
                                            // back to the sign-in form.
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
        }
    }
}
