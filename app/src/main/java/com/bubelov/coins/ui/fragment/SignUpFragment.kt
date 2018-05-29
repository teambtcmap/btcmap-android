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

package com.bubelov.coins.ui.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import com.bubelov.coins.R
import com.bubelov.coins.ui.activity.MapActivity
import com.bubelov.coins.ui.viewmodel.AuthViewModel
import com.bubelov.coins.util.AsyncResult
import dagger.android.support.AndroidSupportInjection

import javax.inject.Inject

import kotlinx.android.synthetic.main.fragment_sign_up.*
import org.jetbrains.anko.alert

class SignUpFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject internal lateinit var modelFactory: ViewModelProvider.Factory

    private val model by lazy {
        ViewModelProviders.of(this)[AuthViewModel::class.java]
    }

    private val authObserver = Observer<AsyncResult<Any>> {
        when (it) {
            is AsyncResult.Loading -> {
                setLoading(true)
            }

            is AsyncResult.Success -> {
                startActivity(Intent(activity, MapActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            }

            is AsyncResult.Error -> {
                setLoading(false)
                activity?.alert { message = it.t.message ?: getString(R.string.something_went_wrong) }?.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        last_name.setOnEditorActionListener(this)

        sign_up.setOnClickListener {
            signUp(
                email.text.toString(),
                password.text.toString(),
                first_name.text.toString(),
                last_name.text.toString()
            )
        }

        model.authState.observe(this, authObserver)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signUp(
                email.text.toString(),
                password.text.toString(),
                first_name.text.toString(),
                last_name.text.toString()
            )

            return true
        }

        return false
    }

    private fun signUp(email: String, password: String, firstName: String, lastName: String) {
        model.signUp(email, password, firstName, lastName).observe(this, authObserver)
    }

    private fun setLoading(loading: Boolean) {
        state_switcher.displayedChild = if (loading) 1 else 0
    }
}