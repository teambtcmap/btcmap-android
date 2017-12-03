package com.bubelov.coins.ui.fragment

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import com.bubelov.coins.R
import com.bubelov.coins.repository.user.SignInResult
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.ui.activity.MapActivity
import dagger.android.AndroidInjection

import javax.inject.Inject

import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.alert

/**
 * @author Igor Bubelov
 */

class SignInFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject lateinit internal var userRepository: UserRepository

    override fun onAttach(context: Context) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        password.setOnEditorActionListener(this)
        sign_in.setOnClickListener { signInNew(email.text.toString(), password.text.toString()) }
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signInNew(email.text.toString(), password.text.toString())
            return true
        }

        return false
    }

    private fun signInNew(email: String, password: String) = launch(UI) {
        sign_in_panel.visibility = View.GONE
        progress.visibility = View.VISIBLE

        val signInResult = async { userRepository.signIn(email, password) }.await()

        when (signInResult) {
            is SignInResult.Success -> startActivity(Intent(activity, MapActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            is SignInResult.Error -> alert { message = signInResult.e.message ?: getString(R.string.something_went_wrong) }.show()
        }

        sign_in_panel.visibility = View.VISIBLE
        progress.visibility = View.GONE
    }
}