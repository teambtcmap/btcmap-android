package com.bubelov.coins.ui.fragment

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
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.ui.activity.AbstractActivity
import com.bubelov.coins.ui.activity.MainActivity

import javax.inject.Inject

import kotlinx.android.synthetic.main.fragment_sign_in.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * @author Igor Bubelov
 */

class SignInFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject
    lateinit var userRepository: UserRepository

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Injector.appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        password.setOnEditorActionListener(this)
        sign_in.setOnClickListener { signIn(email.text.toString(), password.text.toString()) }
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signIn(email.text.toString(), password.text.toString())
            return true
        }

        return false
    }

    private fun signIn(email: String, password: String) {
        doAsync {
            val success = userRepository.signIn(email, password)

            uiThread {
                if (success) {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                } else {
                    val abstractActivity = activity as AbstractActivity
                    abstractActivity.showAlert(R.string.could_not_connect_to_server)
                }
            }
        }
    }
}