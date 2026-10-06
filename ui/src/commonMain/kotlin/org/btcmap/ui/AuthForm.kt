package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AuthImeAction { Next, Done }

/** Test tag prefix so instrumented tests can drive a field by its key. */
const val AUTH_FIELD_TAG_PREFIX = "auth-field-"

/** One field of a credential form. */
data class AuthField(
    val key: String,
    val label: String,
    val value: String,
    val isPassword: Boolean,
    val error: String? = null,
    val helper: String? = null,
    val imeAction: AuthImeAction = AuthImeAction.Next,
    /**
     * Overrides the keyboard type. Defaults to [KeyboardType.Password] for a
     * password field and [KeyboardType.Text] otherwise.
     */
    val keyboardType: KeyboardType? = null,
    /** Autofill hint, so password managers can offer the right credential. */
    val contentType: ContentType? = null,
    /** Explicit test tag; defaults to [AUTH_FIELD_TAG_PREFIX] plus [key]. */
    val testTag: String? = null,
)

/**
 * The credential fields shared by the sign-in, sign-up and change-password
 * forms. Values are hoisted; [onDone] fires from the last field's keyboard.
 */
@Composable
fun AuthFormContent(
    fields: List<AuthField>,
    onValueChange: (key: String, value: String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 24.dp,
        end = 24.dp,
        top = 16.dp,
        bottom = 16.dp,
    ),
    fieldSpacing: Dp = 16.dp,
    /** Announced by a hidden password's toggle; it turns the field visible. */
    showPassword: String = "Show password",
    /** Announced by a visible password's toggle; it masks the field again. */
    hidePassword: String = "Hide password",
) {
    Column(modifier = modifier.padding(contentPadding)) {
        fields.forEachIndexed { index, field ->
            var visible by remember(field.key) { mutableStateOf(false) }

            val supporting: (@Composable () -> Unit)? = when {
                field.error != null -> ({ Text(field.error) })
                field.helper != null -> ({ Text(field.helper) })
                else -> null
            }

            val fieldContentType = field.contentType
            val keyboardType = field.keyboardType
                ?: if (field.isPassword) KeyboardType.Password else KeyboardType.Text

            OutlinedTextField(
                value = field.value,
                onValueChange = { onValueChange(field.key, it) },
                label = { Text(field.label) },
                isError = field.error != null,
                supportingText = supporting,
                singleLine = true,
                visualTransformation = if (field.isPassword && !visible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = if (field.imeAction == AuthImeAction.Done) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    },
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                trailingIcon = if (field.isPassword) {
                    {
                        IconButton(onClick = { visible = !visible }) {
                            MaterialSymbol(
                                glyph = if (visible) "visibility_off" else "visibility",
                                contentDescription = if (visible) hidePassword else showPassword,
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (index == 0) 0.dp else fieldSpacing)
                    .then(
                        if (fieldContentType != null) {
                            Modifier.semantics { contentType = fieldContentType }
                        } else {
                            Modifier
                        }
                    )
                    .testTag(field.testTag ?: (AUTH_FIELD_TAG_PREFIX + field.key)),
            )
        }
    }
}
