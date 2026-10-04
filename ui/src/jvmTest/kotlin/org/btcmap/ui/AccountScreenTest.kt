package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/** The account page's signed-out form: sign-in and sign-up. */
class AccountScreenTest {

    private val db = testDatabase()
    private val settings = testSettings(db)

    private val labels = AccountLabels(
        username = "Username",
        password = "Password",
        confirmPassword = "Confirm password",
        required = "Required",
        passwordTooShort = { "At least $it characters" },
        passwordsDoNotMatch = "Passwords do not match",
        signIn = "Sign in",
        createAccount = "Create account",
        alreadyHaveAccount = "I already have an account",
        createAnAccount = "Create an account",
        accountCreated = "Account created. Please sign in.",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signedOut_showsTheSignInForm() {
        runComposeUiTest {
            setContent {
                AccountScreen(
                    api = testApi(),
                    db = db,
                    settings = settings,
                    tokenLabel = "test",
                    labels = labels,
                    profile = {},
                )
            }
            onNodeWithText("Username").assertIsDisplayed()
            onNodeWithText("Password").assertIsDisplayed()
            onNodeWithText("Sign in").assertIsNotEnabled()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signUp_validatesThePasswordAndConfirmation() {
        runComposeUiTest {
            setContent {
                AccountScreen(
                    api = testApi(),
                    db = db,
                    settings = settings,
                    tokenLabel = "test",
                    labels = labels,
                    profile = {},
                )
            }
            // The sign-up form adds the confirmation field.
            onNodeWithTag(ACCOUNT_TOGGLE_TAG).performClick()
            onNodeWithText("Confirm password").assertIsDisplayed()
            onNodeWithText("Create account").assertIsNotEnabled()

            // A too-short password and a mismatch are both reported, and the
            // request never runs.
            onNodeWithTag(ACCOUNT_USERNAME_TAG).performTextInput("alice")
            onNodeWithTag(ACCOUNT_PASSWORD_TAG).performTextInput("short")
            onNodeWithTag(ACCOUNT_CONFIRM_TAG).performTextInput("different")
            onNodeWithTag(ACCOUNT_SUBMIT_TAG).performClick()

            onNodeWithText("Passwords do not match").assertIsDisplayed()
        }
    }
}
