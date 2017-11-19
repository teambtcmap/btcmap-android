package com.bubelov.coins.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem

import com.bubelov.coins.R
import com.bubelov.coins.repository.user.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso
import dagger.android.AndroidInjection

import javax.inject.Inject

import kotlinx.android.synthetic.main.activity_profile.*

/**
 * @author Igor Bubelov
 */

class ProfileActivity : AbstractActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar.inflateMenu(R.menu.profile)
        toolbar.setOnMenuItemClickListener(this)

        val user = userRepository.user!!

        if (!TextUtils.isEmpty(user.avatarUrl)) {
            Picasso.with(this).load(user.avatarUrl).into(avatar)
        } else {
            avatar.setImageResource(R.drawable.ic_no_avatar)
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
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        googleSignInClient.signOut().addOnCompleteListener {
            onSignOut()
        }
    }

    private fun onSignOut() {
        userRepository.clear()
        setResult(RESULT_SIGN_OUT)
        supportFinishAfterTransition()
    }

    companion object {
        val RESULT_SIGN_OUT = 10

        fun newIntent(context: Context): Intent = Intent(context, ProfileActivity::class.java)
    }
}