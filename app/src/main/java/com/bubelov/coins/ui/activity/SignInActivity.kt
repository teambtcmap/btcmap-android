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
import com.google.android.gms.auth.api.signin.*
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
import com.google.android.gms.auth.api.signin.GoogleSignIn

/**
 * @author Igor Bubelov
 */

class SignInActivity : AbstractActivity(), GoogleApiClient.OnConnectionFailedListener {
    @Inject lateinit var userRepository: UserRepository

    @Inject lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        sign_in_with_google.setOnClickListener {
            val googleSingInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
                    .requestEmail()
                    .build()

            val googleSignInClient = GoogleSignIn.getClient(this, googleSingInOptions)
            startActivityForResult(googleSignInClient.signInIntent, REQUEST_GOOGLE_SIGN_IN)
        }

        sign_in_with_email.setOnClickListener {
            val intent = Intent(this, EmailSignInActivity::class.java)
            startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_GOOGLE_SIGN_IN) {
            signIn(GoogleSignIn.getSignedInAccountFromIntent(data).result.idToken!!)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Toast.makeText(this, R.string.cant_connect_to_google_services, Toast.LENGTH_LONG).show()
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

        fun newIntent(context: Context): Intent = Intent(context, SignInActivity::class.java)
    }
}