package org.btcmap.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputLayout
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the sign-up form so the field-level validation is exercised on a real
 * view hierarchy. Validation failures stay inside the dialog (no request is
 * made), which makes this a pure UI test.
 */
@RunWith(AndroidJUnit4::class)
class AuthDialogValidationTest {

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

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
                    host.showAuthDialog { }
                }

                // Sign-up form, submitted empty: both fields report an error.
                onView(withId(R.id.createAccountOption)).inRoot(isDialog()).perform(click())
                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

                onView(withId(R.id.usernameInput)).inRoot(isDialog())
                    .check(matches(withInputError(required)))
                onView(withId(R.id.passwordInput)).inRoot(isDialog())
                    .check(matches(withInputError(required)))

                // Fill only the username and submit again. The username error must
                // be gone even though the password still fails validation.
                onView(withId(R.id.usernameInput)).inRoot(isDialog())
                    .perform(typeText("satoshi"), closeSoftKeyboard())
                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())

                onView(withId(R.id.usernameInput)).inRoot(isDialog())
                    .check(matches(withInputError(null)))
                onView(withId(R.id.passwordInput)).inRoot(isDialog())
                    .check(matches(withInputError(required)))
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }

    class AuthHostFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            return FrameLayout(requireContext())
        }
    }

    companion object {
        private const val HOST_TAG = "auth-validation-host"
        private const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

        /**
         * Matches a [android.widget.EditText] by the error its [TextInputLayout]
         * (or the edit text itself) is showing. A null [error] matches only when
         * no error is set.
         */
        private fun withInputError(error: String?): TypeSafeMatcher<View> =
            object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description) {
                    description.appendText("with input error ${error ?: "<none>"}")
                }

                override fun matchesSafely(view: View): Boolean = inputError(view)?.toString() == error

                private fun inputError(view: View): CharSequence? {
                    if (view is EditText && view.error != null) return view.error
                    var parent = view.parent
                    while (parent != null) {
                        if (parent is TextInputLayout) return parent.error
                        parent = parent.parent
                    }
                    return null
                }
            }
    }
}
