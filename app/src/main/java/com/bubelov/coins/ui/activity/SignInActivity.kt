/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.ui.activity

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bubelov.coins.BuildConfig

import com.bubelov.coins.R
import com.bubelov.coins.ui.viewmodel.AuthViewModel
import com.bubelov.coins.util.AsyncResult
import com.google.android.gms.auth.api.signin.*
import dagger.android.AndroidInjection

import javax.inject.Inject

import kotlinx.android.synthetic.main.activity_sign_in.*
import org.jetbrains.anko.alert
import com.google.android.gms.auth.api.signin.GoogleSignIn

class SignInActivity : AppCompatActivity() {
    @Inject internal lateinit var modelFactory: ViewModelProvider.Factory

    val model by lazy {
        ViewModelProviders.of(this, modelFactory)[AuthViewModel::class.java]
    }

    private val authObserver = Observer<AsyncResult<Any>> {
        when (it) {
            is AsyncResult.Loading -> {
                setLoading(true)
            }

            is AsyncResult.Success -> {
                setResult(Activity.RESULT_OK)
                finish()
            }

            is AsyncResult.Error -> {
                setLoading(false)
                alert { message = it.t.message ?: getString(R.string.something_went_wrong) }.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        sign_in_with_google.setOnClickListener {
            val googleSingInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
                    .requestEmail()
                    .build()

            val googleSignInClient = GoogleSignIn.getClient(this, googleSingInOptions)
            startActivityForResult(googleSignInClient.signInIntent, GOOGLE_SIGN_IN_REQUEST)
        }

        sign_in_with_email.setOnClickListener {
            startActivity(EmailSignInActivity.newIntent(this))
        }

        model.authState.observe(this, authObserver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST && resultCode == Activity.RESULT_OK) {
            model.signIn(GoogleSignIn.getSignedInAccountFromIntent(data).result.idToken!!).observe(
                this,
                authObserver
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        view_switcher.displayedChild = if (loading) 1 else 0
    }

    companion object {
        private const val GOOGLE_SIGN_IN_REQUEST = 10

        fun newIntent(context: Context) = Intent(context, SignInActivity::class.java)
    }
}