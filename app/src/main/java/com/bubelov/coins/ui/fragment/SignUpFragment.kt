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

import kotlinx.android.synthetic.main.fragment_sign_up.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.alert

/**
 * @author Igor Bubelov
 */

class SignUpFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject internal lateinit var userRepository: UserRepository

    override fun onAttach(context: Context) {
        AndroidInjection.inject(this)
        super.onAttach(context)
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

    private fun signUp(email: String, password: String, firstName: String, lastName: String) = launch(UI) {
        setLoading(true)

        val result = async { userRepository.signUp(email, password, firstName, lastName) }.await()

        when (result) {
            is SignInResult.Success -> startActivity(Intent(activity, MapActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })

            is SignInResult.Error -> {
                setLoading(false)
                alert { message = result.e.message ?: getString(R.string.something_went_wrong) }.show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        state_switcher.displayedChild = if (loading) 1 else 0
    }
}