package org.btcmap.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.ui.AUTH_FIELD_TAG_PREFIX
import org.btcmap.util.AppTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the sign-up form so the field-level validation is exercised. Validation
 * failures stay inside the dialog (no request is made), which makes this a pure
 * UI test. The form fields are Compose; the dialog's positive button is a Views
 * button, so it is driven through Espresso.
 */
@RunWith(AndroidJUnit4::class)
class AuthDialogValidationTest : AppTestCase() {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<App>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val required = context.getString(R.string.field_required)

    @Test
    fun signUp_correctedUsername_clearsItsErrorOnResubmit() {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val host = AuthHostFragment()
                    activity.supportFragmentManager.commitNow {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, host, HOST_TAG)
                    }
                    host.showAuthDialog()
                }

                // Sign-up form, submitted empty: both fields report an error.
                composeTestRule
                    .onNodeWithText(context.getString(R.string.i_don_t_have_an_account))
                    .performClick()
                submit()

                composeTestRule.onAllNodesWithText(required, useUnmergedTree = true)
                    .assertCountEquals(2)

                // Fill only the username and submit again. The username error must
                // be gone even though the password still fails validation.
                composeTestRule
                    .onNodeWithTag(AUTH_FIELD_TAG_PREFIX + "username")
                    .performTextInput("satoshi")
                submit()

                composeTestRule.onAllNodesWithText(required, useUnmergedTree = true)
                    .assertCountEquals(1)
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    private fun submit() {
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
    }

    class AuthHostFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            return FrameLayout(requireContext())
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            registerAuthResultListener { }
        }
    }

    companion object {
        private const val HOST_TAG = "auth-validation-host"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"
    }
}
