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
import com.bubelov.coins.ui.activity.MapActivity

import javax.inject.Inject

import kotlinx.android.synthetic.main.fragment_sign_up.*
import org.jetbrains.anko.doAsync

/**
 * @author Igor Bubelov
 */

class SignUpFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject
    lateinit var userRepository: UserRepository

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Injector.mainComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        last_name.setOnEditorActionListener(this)
        sign_up.setOnClickListener { signUp(email.text.toString(), password.text.toString(), first_name.text.toString(), last_name.text.toString()) }
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signUp(email.text.toString(), password.text.toString(), first_name.text.toString(), last_name.text.toString())
            return true
        }

        return false
    }

    private fun signUp(email: String, password: String, firstName: String, lastName: String) {
        setState(STATE_PROGRESS)

        doAsync {
            val success = userRepository.signUp(email, password, firstName, lastName)

            if (success) {
                val intent = Intent(context, MapActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {
                setState(STATE_FILL_FORM) // TODO
            }
        }
    }

    private fun setState(state: Int) {
        state_switcher.displayedChild = state
    }

    companion object {
        private val STATE_FILL_FORM = 0
        private val STATE_PROGRESS = 1
    }
}