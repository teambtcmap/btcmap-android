package com.bubelov.coins.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
import android.widget.Toast

import com.bubelov.coins.R
import com.bubelov.coins.repository.user.UserRepository
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.squareup.picasso.Picasso

import javax.inject.Inject

import kotlinx.android.synthetic.main.activity_profile.*

/**
 * @author Igor Bubelov
 */

class ProfileActivity : AbstractActivity(), Toolbar.OnMenuItemClickListener {
    @Inject
    lateinit var userRepository: UserRepository

    lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        dependencies().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar.inflateMenu(R.menu.profile)
        toolbar.setOnMenuItemClickListener(this)

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .enableAutoManage(this) { connectionResult -> Toast.makeText(this@ProfileActivity, connectionResult.errorMessage, Toast.LENGTH_SHORT).show() }
                .build()

        val user = userRepository.user!!

        if (!TextUtils.isEmpty(user.avatarUrl)) {
            Picasso.with(this).load(user.avatarUrl).into(avatar)
        } else {
            avatar!!.setImageResource(R.drawable.ic_no_avatar)
        }

        if (!TextUtils.isEmpty(user.firstName)) {
            user_name.text = String.format("%s %s", user.firstName, user.lastName)
        } else {
            user_name.text = user.email
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_sign_out) {
            signOut()
            return true
        } else {
            return false
        }
    }

    private fun signOut() {
        if ("google".equals(userRepository.userAuthMethod, ignoreCase = true)) {
            googleSignOut()
        } else {
            onSignOut()
        }
    }

    private fun googleSignOut() {
        if (!googleApiClient.isConnected) {
            showAlert("Couldn't connect to Google services.")
            return
        }

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { onSignOut() }
    }

    private fun onSignOut() {
        userRepository.clear()
        setResult(RESULT_SIGN_OUT)
        supportFinishAfterTransition()
    }

    companion object {
        val RESULT_SIGN_OUT = 10

        fun newIntent(context: Context): Intent {
            return Intent(context, ProfileActivity::class.java)
        }
    }
}