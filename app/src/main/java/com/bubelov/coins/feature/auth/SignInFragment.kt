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

package com.bubelov.coins.feature.auth

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import com.bubelov.coins.R
import com.bubelov.coins.util.AsyncResult
import com.bubelov.coins.util.viewModelProvider
import dagger.android.support.DaggerFragment

import javax.inject.Inject

import kotlinx.android.synthetic.main.fragment_sign_in.*

class SignInFragment : DaggerFragment(), TextView.OnEditorActionListener {
    @Inject internal lateinit var modelFactory: ViewModelProvider.Factory

    private val model by lazy { viewModelProvider(modelFactory) as AuthViewModel }

    private val authObserver = Observer<AsyncResult<Any>> {
        when (it) {
            is AsyncResult.Loading -> {
                sign_in_panel.visibility = View.GONE
                progress.visibility = View.VISIBLE
            }

            is AsyncResult.Success -> {
// TODO
//                startActivity(
//                    Intent(activity, MapActivity::class.java).apply {
//                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                    })
            }

            is AsyncResult.Error -> {
                sign_in_panel.visibility = View.VISIBLE
                progress.visibility = View.GONE

                AlertDialog.Builder(requireContext())
                    .setMessage(it.t.message ?: getString(R.string.something_went_wrong))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        password.setOnEditorActionListener(this)

        sign_in.setOnClickListener {
            model.signIn(email.text.toString(), password.text.toString())
                .observe(this, authObserver)
        }

        model.authState.observe(this, authObserver)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            model.signIn(email.text.toString(), password.text.toString())
                .observe(this, authObserver)
            return true
        }

        return false
    }
}