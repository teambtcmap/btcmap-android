package org.btcmap.ui

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
import org.btcmap.account.AccountSession
import org.btcmap.api.Api
import org.btcmap.api.deletePlaceImage
import org.btcmap.api.getMyEvents
import org.btcmap.api.getMyPlaceImages
import org.btcmap.api.placeImageUrl
import org.btcmap.api.revokeEvent
import org.btcmap.api.signOut
import org.btcmap.api.updatePassword
import org.btcmap.auth.AuthError
import org.btcmap.auth.AuthValidation
import org.btcmap.db.Database
import org.btcmap.db.table.user.User
import org.btcmap.i18n.getLocalizedName
import org.btcmap.saved.SavedItems
import org.btcmap.saved.withLocalizedAreaNames
import org.btcmap.saved.withLocalizedPlaceNames
import org.btcmap.settings.Settings
import org.btcmap.ui.map.EventMiniMap

/** Test tags so a test can drive the profile forms. */
const val PROFILE_USERNAME_FIELD_TAG = "profile-username-field"
const val PROFILE_USERNAME_SAVE_TAG = "profile-username-save"
const val PROFILE_PASSWORD_CURRENT_TAG = "profile-password-current"
const val PROFILE_PASSWORD_NEW_TAG = "profile-password-new"
const val PROFILE_PASSWORD_CONFIRM_TAG = "profile-password-confirm"
const val PROFILE_PASSWORD_SAVE_TAG = "profile-password-save"

/** The profile edit forms' strings, so the screens stay resource-free. */
data class ProfileFormLabels(
    val passwordMask: String,
    val required: String,
    val changeUsernameTitle: String,
    val changePasswordTitle: String,
    val username: String,
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
    val passwordsDoNotMatch: String,
    val passwordTooShort: (minLength: Int) -> String,
    val save: String,
    val cancel: String,
    val usernameChanged: String,
    val passwordChanged: String,
)

/** The profile page the signed-in account sees, or one of its edit forms. */
private enum class ProfileEdit { Username, Password }

/**
 * The signed-in account page: the shared [UserProfileScreen] over the cached
 * user, with inline change-username and change-password forms and a log-out that
 * clears the session and (best-effort) revokes the token server-side.
 *
 * The edit forms are inline rather than dialogs so they render in one composable
 * window, which keeps them testable. [profileLabels] are [UserProfileScreen]'s
 * strings; [formLabels] are the edit forms'.
 */
@Composable
fun ProfileScreen(
    api: Api,
    db: Database,
    settings: Settings,
    profileLabels: UserProfileLabels,
    formLabels: ProfileFormLabels,
    imagesLabels: UploadedImagesLabels,
    eventsLabels: MyEventsLabels,
    /** The map style the my-events previews render with. */
    mapStyleUrl: String,
    mapStyleJson: String?,
    showUploadedImages: Boolean,
    onShowUploadedImagesChange: (Boolean) -> Unit,
    showMyEvents: Boolean,
    onShowMyEventsChange: (Boolean) -> Unit,
    /** Opens the add-event screen pre-filled from one of the user's events. */
    onDuplicateEvent: (MyEventUi) -> Unit = {},
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
                savedPlaces = user.savedPlaces.withLocalizedPlaceNames(db),
                savedAreas = user.savedAreas.withLocalizedAreaNames(db),
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

        showUploadedImages -> UploadedImagesScreen(
            labels = imagesLabels,
            load = {
                api.getMyPlaceImages().map { image ->
                    UploadedImageUi(
                        placeId = image.placeId,
                        imageId = image.id,
                        thumbnailUrl = api.placeImageUrl(
                            placeId = image.placeId,
                            imageId = image.id,
                            width = PLACE_PHOTO_THUMBNAIL_SIZE,
                            height = PLACE_PHOTO_THUMBNAIL_SIZE,
                        ),
                        placeName = withContext(Dispatchers.IO) {
                            db.place.selectById(image.placeId)?.getLocalizedName()
                        } ?: imagesLabels.unknownPlace(image.placeId),
                    )
                }
            },
            delete = { image ->
                api.deletePlaceImage(placeId = image.placeId, imageId = image.imageId)
            },
        )

        showMyEvents -> MyEventsScreen(
            labels = eventsLabels,
            load = {
                api.getMyEvents().map { event ->
                    MyEventUi(
                        id = event.id,
                        lat = event.lat,
                        lon = event.lon,
                        name = event.name,
                        website = event.website?.toString().orEmpty(),
                        startsAt = event.startsAt,
                        endsAt = event.endsAt,
                        status = MyEventStatus.fromValue(event.status),
                        startsAtLocal = event.startsAtLocal,
                        endsAtLocal = event.endsAtLocal,
                    )
                }
            },
            revoke = { event -> api.revokeEvent(event.id) },
            onDuplicate = onDuplicateEvent,
            map = { event, mapModifier ->
                EventMiniMap(
                    lat = event.lat,
                    lon = event.lon,
                    styleUrl = mapStyleUrl,
                    styleJson = mapStyleJson,
                    palette = markerPalette(settings),
                    modifier = mapModifier,
                )
            },
        )

        editing == ProfileEdit.Username -> ChangeUsernameForm(
            currentName = current.name,
            labels = formLabels,
            onCancel = { editing = null },
            save = { name ->
                AccountSession.changeUsername(api, db, name)
                message = formLabels.usernameChanged
                reload()
            },
        )

        editing == ProfileEdit.Password -> ChangePasswordForm(
            labels = formLabels,
            onCancel = { editing = null },
            save = { oldPassword, newPassword ->
                api.updatePassword(oldPassword = oldPassword, newPassword = newPassword)
                message = formLabels.passwordChanged
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
            ContentColumn(
                modifier = Modifier.weight(1f),
                maxWidth = CONTENT_MAX_WIDTH,
            ) {
                UserProfileScreen(
                    state = UserProfileUiState(
                        username = current.name,
                        password = formLabels.passwordMask,
                        savedPlaces = current.savedPlaces.map { SavedItemUi(it.id, it.name) },
                        savedAreas = current.savedAreas.map { SavedItemUi(it.id, it.name) },
                        labels = profileLabels,
                    ),
                    onEditUsername = {
                        message = null
                        editing = ProfileEdit.Username
                    },
                    onEditPassword = {
                        message = null
                        editing = ProfileEdit.Password
                    },
                    onOpenUploadedImages = {
                        message = null
                        onShowUploadedImagesChange(true)
                    },
                    onOpenMyEvents = {
                        message = null
                        onShowMyEventsChange(true)
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
                )
            }
        }
    }
}

/** The change-username form: one field, saved when it is not blank. */
@Composable
fun ChangeUsernameForm(
    currentName: String,
    labels: ProfileFormLabels,
    onCancel: () -> Unit,
    save: suspend (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ContentColumn(
        maxWidth = CONTENT_MAX_WIDTH,
        scroll = true,
        margin = true,
        modifier = Modifier.padding(vertical = 16.dp),
    ) {
        Text(text = labels.changeUsernameTitle, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            label = { Text(labels.username) },
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
                        error = labels.required
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
                Text(labels.save)
            }
            TextButton(onClick = onCancel, enabled = !busy) { Text(labels.cancel) }
        }
    }
}

/** The change-password form: current, new and confirmation, client-validated. */
@Composable
fun ChangePasswordForm(
    labels: ProfileFormLabels,
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

    ContentColumn(
        maxWidth = CONTENT_MAX_WIDTH,
        scroll = true,
        margin = true,
        modifier = Modifier.padding(vertical = 16.dp),
    ) {
        Text(text = labels.changePasswordTitle, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text(labels.currentPassword) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = currentError,
            supportingText = if (currentError) {
                { Text(labels.required) }
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
            label = { Text(labels.newPassword) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = newError,
            supportingText = {
                when {
                    newTooShort -> Text(labels.passwordTooShort(AuthValidation.MIN_PASSWORD_LENGTH))
                    newError -> Text(labels.required)
                    else -> Text(labels.passwordTooShort(AuthValidation.MIN_PASSWORD_LENGTH))
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
            label = { Text(labels.confirmPassword) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmError,
            supportingText = if (confirmError) {
                { Text(labels.passwordsDoNotMatch) }
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
                Text(labels.save)
            }
            TextButton(onClick = onCancel, enabled = !busy) { Text(labels.cancel) }
        }
    }
}

/**
 * Clears the session locally and revokes the token server-side best-effort. Only
 * the token this screen saw is cleared, so a sign-in that raced the logout is
 * not dropped.
 */
private suspend fun logOut(api: Api, db: Database, settings: Settings) {
    AccountSession.clearSession(db, settings)?.let { token ->
        runCatching { api.signOut(token) }
    }
}
