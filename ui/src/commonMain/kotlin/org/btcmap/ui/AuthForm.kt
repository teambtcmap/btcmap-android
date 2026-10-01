package org.btcmap.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
) {
    Column(modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)) {
        fields.forEachIndexed { index, field ->
            var visible by remember(field.key) { mutableStateOf(false) }

            val supporting: (@Composable () -> Unit)? = when {
                field.error != null -> ({ Text(field.error) })
                field.helper != null -> ({ Text(field.helper) })
                else -> null
            }

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
                                contentDescription = null,
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (index == 0) 0.dp else 16.dp)
                    .testTag(AUTH_FIELD_TAG_PREFIX + field.key),
            )
        }
    }
}
