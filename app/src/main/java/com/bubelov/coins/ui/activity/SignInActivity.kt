package com.bubelov.coins.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.widget.Toast

import com.bubelov.coins.BuildConfig
import com.bubelov.coins.R
import com.bubelov.coins.repository.user.UserRepository
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.analytics.FirebaseAnalytics

import javax.inject.Inject

import kotlinx.android.synthetic.main.activity_sign_in.*

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
        dependencies().inject(this)
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
            val account = result.signInAccount
            AuthWithGoogleTask(account!!.idToken!!).execute()
        }
    }

    private fun setLoading(loading: Boolean) {
        view_switcher.displayedChild = if (loading) 1 else 0
    }

    private inner class AuthWithGoogleTask(private val token: String) : AsyncTask<Void, Void, Boolean>() {
        override fun onPreExecute() {
            setLoading(true)
        }

        override fun doInBackground(vararg params: Void): Boolean? {
            return userRepository.signIn(token)
        }

        override fun onPostExecute(result: Boolean?) {
            if (result == null) {
                setLoading(false)
                Toast.makeText(this@SignInActivity, R.string.could_not_connect_to_server, Toast.LENGTH_SHORT).show()
                return
            }

            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, "google")
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)

            setResult(Activity.RESULT_OK)
            supportFinishAfterTransition()
        }
    }

    companion object {
        private val REQUEST_GOOGLE_SIGN_IN = 10

        fun newIntent(context: Context): Intent {
            return Intent(context, SignInActivity::class.java)
        }
    }
}