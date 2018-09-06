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

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.bubelov.coins.R
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_profile.*
import androidx.navigation.fragment.findNavController
import com.bubelov.coins.repository.user.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.squareup.picasso.Picasso
import javax.inject.Inject

class ProfileFragment : DaggerFragment(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        toolbar.inflateMenu(R.menu.profile)
        toolbar.setOnMenuItemClickListener(this)

        val user = userRepository.user!!

        if (!TextUtils.isEmpty(user.avatarUrl)) {
            Picasso.with(requireContext()).load(user.avatarUrl).into(avatar)
        } else {
            avatar.setImageResource(R.drawable.ic_no_avatar)
        }

        if (!TextUtils.isEmpty(user.firstName)) {
            userName.text = String.format("%s %s", user.firstName, user.lastName)
        } else {
            userName.text = user.email
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_sign_out) {
            signOut()
            true
        } else {
            false
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
        val googleSignInClient =
            GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)

        googleSignInClient.signOut().addOnCompleteListener {
            onSignOut()
        }
    }

    private fun onSignOut() {
        userRepository.clear()
        findNavController().popBackStack()
    }
}