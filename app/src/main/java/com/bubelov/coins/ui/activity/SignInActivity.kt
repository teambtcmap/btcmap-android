package com.bubelov.coins.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.widget.Toast

import com.bubelov.coins.BuildConfig
import com.bubelov.coins.R
import com.bubelov.coins.repository.user.SignInResult
import com.bubelov.coins.repository.user.UserRepository
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.AndroidInjection

import javax.inject.Inject

import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.alert

/**
 * @author Igor Bubelov
 */

class SignInActivity : AbstractActivity(), GoogleApiClient.OnConnectionFailedListener {
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var analytics: FirebaseAnalytics

    lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
                .requestEmail()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions)
                .addApi(Auth.CREDENTIALS_API)
                .build()

        sign_in_with_google.setOnClickListener {
            val intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
            startActivityForResult(intent, REQUEST_GOOGLE_SIGN_IN)
        }

        sign_in_with_email.setOnClickListener {
            val intent = Intent(this, EmailSignInActivity::class.java)
            startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    public override fun onStart() {
        super.onStart()

        val pendingResult = Auth.GoogleSignInApi.silentSignIn(googleApiClient)

        if (pendingResult.isDone) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            val result = pendingResult.get()
            handleSignInResult(result)
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently. Cross-device
            // single sign-on will occur in this branch.
            sign_in_with_google.isEnabled = false
            pendingResult.setResultCallback { googleSignInResult ->
                sign_in_with_google.isEnabled = true
                handleSignInResult(googleSignInResult)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_GOOGLE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Toast.makeText(this, R.string.cant_connect_to_google_services, Toast.LENGTH_LONG).show()
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            signIn(result.signInAccount!!.idToken!!)
        }
    }

    private fun signIn(idToken: String) = launch(UI) {
        setLoading(true)

        val signInResult = async { userRepository.signIn(idToken) }.await()

        when (signInResult) {
            is SignInResult.Success -> {
                val bundle = Bundle().apply { putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, "google") }
                analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
                setResult(Activity.RESULT_OK)
                supportFinishAfterTransition()
            }

            is SignInResult.Error -> {
                setLoading(false)
                alert { message = signInResult.e.message ?: getString(R.string.something_went_wrong) }.show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        view_switcher.displayedChild = if (loading) 1 else 0
    }

    companion object {
        private val REQUEST_GOOGLE_SIGN_IN = 10

        fun newIntent(context: Context): Intent {
            return Intent(context, SignInActivity::class.java)
        }
    }
}