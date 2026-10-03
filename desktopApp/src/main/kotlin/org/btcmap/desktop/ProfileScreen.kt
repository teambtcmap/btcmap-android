package org.btcmap.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api.Api
import org.btcmap.api.signOut
import org.btcmap.api.toDbUser
import org.btcmap.api.updatePassword
import org.btcmap.api.updateUsername
import org.btcmap.auth.AuthError
import org.btcmap.auth.AuthValidation
import org.btcmap.saved.SavedItems
import org.btcmap.db.Database
import org.btcmap.db.table.user.User
import org.btcmap.i18n.getLocalizedName
import org.btcmap.settings.Settings
import org.btcmap.settings.authToken
import org.btcmap.ui.SavedItemUi
import org.btcmap.ui.UserProfileLabels
import org.btcmap.ui.UserProfileScreen
import org.btcmap.ui.UserProfileUiState

/** Test tags so the desktop tests can drive the profile forms. */
internal const val PROFILE_USERNAME_FIELD_TAG = "profile-username-field"
internal const val PROFILE_USERNAME_SAVE_TAG = "profile-username-save"
internal const val PROFILE_PASSWORD_CURRENT_TAG = "profile-password-current"
internal const val PROFILE_PASSWORD_NEW_TAG = "profile-password-new"
internal const val PROFILE_PASSWORD_CONFIRM_TAG = "profile-password-confirm"
internal const val PROFILE_PASSWORD_SAVE_TAG = "profile-password-save"

private const val PASSWORD_MASK = "••••••••"
private const val REQUIRED = "Required"

/** The profile page the signed-in account sees, or one of its edit forms. */
private enum class ProfileEdit { Username, Password }

private val PROFILE_LABELS = UserProfileLabels(
    username = "Username",
    password = "Password",
    savedPlaces = "Saved places",
    savedAreas = "Saved areas",
    noSavedPlaces = "No saved places.",
    noSavedAreas = "No saved areas.",
    logOut = "Log out",
    editUsername = "Change username",
    editPassword = "Change password",
    delete = "Delete",
)

/**
 * The signed-in account page: the shared [UserProfileScreen] over the cached
 * user, with inline change-username and change-password forms and a log-out that
 * clears the session and (best-effort) revokes the token server-side.
 *
 * The edit forms are inline rather than dialogs so they render in one composable
 * window, which keeps them testable.
 */
@Composable
internal fun DesktopProfile(
    api: Api,
    db: Database,
    settings: Settings,
    onLoggedOut: () -> Unit,
) {
    var account by remember { mutableStateOf<User?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProfileEdit?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        account = withContext(Dispatchers.IO) {
            val user = db.user.select() ?: return@withContext null
            user.copy(
                savedPlaces = user.savedPlaces.map {
                    it.copy(name = localName(db, it.id, isPlace = true) ?: it.name)
                },
                savedAreas = user.savedAreas.map {
                    it.copy(name = localName(db, it.id, isPlace = false) ?: it.name)
                },
            )
        }
        loaded = true
    }

    LaunchedEffect(Unit) { reload() }

    val current = account
    when {
        !loaded -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        // No cached account means no usable session; clear any leftover token
        // instead of leaving it behind, and fall back to the sign-in form.
        current == null -> LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) { settings.clearSession(db) }
            onLoggedOut()
        }

        editing == ProfileEdit.Username -> ChangeUsernameForm(
            currentName = current.name,
            onCancel = { editing = null },
            save = { name ->
                changeUsername(api, db, name)
                message = "Username changed."
                reload()
            },
        )

        editing == ProfileEdit.Password -> ChangePasswordForm(
            onCancel = { editing = null },
            save = { oldPassword, newPassword ->
                api.updatePassword(oldPassword = oldPassword, newPassword = newPassword)
                message = "Password changed."
            },
        )

        else -> Column(modifier = Modifier.fillMaxSize()) {
            message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            UserProfileScreen(
                state = UserProfileUiState(
                    username = current.name,
                    password = PASSWORD_MASK,
                    savedPlaces = current.savedPlaces.map { SavedItemUi(it.id, it.name) },
                    savedAreas = current.savedAreas.map { SavedItemUi(it.id, it.name) },
                    labels = PROFILE_LABELS,
                ),
                onEditUsername = {
                    message = null
                    editing = ProfileEdit.Username
                },
                onEditPassword = {
                    message = null
                    editing = ProfileEdit.Password
                },
                onDeletePlace = { id ->
                    scope.launch {
                        SavedItems.removePlace(api, db, id)
                        reload()
                    }
                },
                onDeleteArea = { id ->
                    scope.launch {
                        SavedItems.removeArea(api, db, id)
                        reload()
                    }
                },
                onLogOut = {
                    scope.launch {
                        logOut(api, db, settings)
                        onLoggedOut()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The change-username form: one field, saved when it is not blank. */
@Composable
internal fun ChangeUsernameForm(
    currentName: String,
    onCancel: () -> Unit,
    save: suspend (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Change username", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            label = { Text("Username") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(PROFILE_USERNAME_FIELD_TAG),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Button(
                enabled = !busy,
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = REQUIRED
                    } else {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                save(trimmed)
                                onCancel()
                            } catch (t: Throwable) {
                                error = t.message ?: t.toString()
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                modifier = Modifier.testTag(PROFILE_USERNAME_SAVE_TAG),
            ) {
                Text("Save")
            }
            TextButton(onClick = onCancel, enabled = !busy) { Text("Cancel") }
        }
    }
}

/** The change-password form: current, new and confirmation, client-validated. */
@Composable
internal fun ChangePasswordForm(
    onCancel: () -> Unit,
    save: suspend (current: String, new: String) -> Unit,
) {
    var current by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val errors = if (attempted) {
        AuthValidation.changePassword(current, newPassword, confirmation)
    } else {
        emptyList()
    }
    val currentError = errors.contains(AuthError.CurrentPasswordRequired)
    val newTooShort = errors.contains(AuthError.PasswordTooShort)
    val newError = newTooShort || errors.contains(AuthError.PasswordRequired)
    val confirmError = errors.contains(AuthError.PasswordsDoNotMatch)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Change password", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Current password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = currentError,
            supportingText = if (currentError) {
                { Text(REQUIRED) }
            } else {
                null
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(PROFILE_PASSWORD_CURRENT_TAG),
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = newError,
            supportingText = {
                when {
                    newTooShort -> Text("At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters")
                    newError -> Text(REQUIRED)
                    else -> Text("At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters")
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(PROFILE_PASSWORD_NEW_TAG),
        )
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmError,
            supportingText = if (confirmError) {
                { Text("Passwords do not match") }
            } else {
                null
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(PROFILE_PASSWORD_CONFIRM_TAG),
        )
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Button(
                enabled = !busy,
                onClick = {
                    attempted = true
                    if (AuthValidation.changePassword(current, newPassword, confirmation).isEmpty()) {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                save(current, newPassword)
                                onCancel()
                            } catch (t: Throwable) {
                                error = t.message ?: t.toString()
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                modifier = Modifier.testTag(PROFILE_PASSWORD_SAVE_TAG),
            ) {
                Text("Save")
            }
            TextButton(onClick = onCancel, enabled = !busy) { Text("Cancel") }
        }
    }
}

/** The cached name of a saved entity, localized when the local cache has it. */
private suspend fun localName(db: Database, id: Long, isPlace: Boolean): String? =
    withContext(Dispatchers.IO) {
        val localized = if (isPlace) {
            db.place.selectById(id)?.getLocalizedName()
        } else {
            db.area.selectById(id)?.getLocalizedName()
        }
        localized?.takeIf { it.isNotBlank() }
    }

/**
 * Renames the account, keeping the cached saved lists: the username endpoint
 * returns them empty, so the cached ones are copied into the updated user.
 */
private suspend fun changeUsername(api: Api, db: Database, name: String) {
    val updated = api.updateUsername(name).toDbUser()
    withContext(Dispatchers.IO) {
        val existing = db.user.select()
        db.transaction {
            db.user.delete()
            db.user.insert(
                updated.copy(
                    savedPlaces = existing?.savedPlaces ?: updated.savedPlaces,
                    savedAreas = existing?.savedAreas ?: updated.savedAreas,
                ),
            )
        }
    }
}

/**
 * Clears the session locally and revokes the token server-side best-effort. Only
 * the token this screen saw is cleared, so a sign-in that raced the logout is
 * not dropped.
 */
private suspend fun logOut(api: Api, db: Database, settings: Settings) {
    val token = settings.authToken
    withContext(Dispatchers.IO) {
        if (token.isNullOrBlank()) {
            settings.clearSession(db)
        } else {
            settings.clearSessionIfTokenMatches(db, token)
        }
    }
    if (!token.isNullOrBlank()) {
        runCatching { api.signOut(token) }
    }
}
