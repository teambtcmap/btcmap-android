package com.bubelov.coins.ui.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.util.Pair

import com.bubelov.coins.R
import com.bubelov.coins.ui.fragment.SignInFragment
import com.bubelov.coins.ui.fragment.SignUpFragment

import java.util.ArrayList

import kotlinx.android.synthetic.main.activity_email_sign_in.*

/**
 * @author Igor Bubelov
 */

class EmailSignInActivity : AbstractActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sign_in)
        pager.adapter = TabsAdapter(supportFragmentManager)
        tab_layout.setupWithViewPager(pager)
        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
    }

    private inner class TabsAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val pages: MutableList<Pair<Fragment, String>>

        init {
            pages = ArrayList<Pair<Fragment, String>>()
            pages.add(Pair<Fragment, String>(SignInFragment(), getString(R.string.sign_in)))
            pages.add(Pair<Fragment, String>(SignUpFragment(), getString(R.string.sign_up)))
        }

        override fun getItem(position: Int): Fragment {
            return pages[position].first
        }

        override fun getPageTitle(position: Int): CharSequence {
            return pages[position].second
        }

        override fun getCount(): Int {
            return pages.size
        }
    }
}