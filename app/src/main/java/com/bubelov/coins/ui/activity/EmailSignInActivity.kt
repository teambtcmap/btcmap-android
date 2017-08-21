package com.bubelov.coins.ui.activity

import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.support.v4.util.Pair

import com.bubelov.coins.R
import com.bubelov.coins.ui.fragment.SignInFragment
import com.bubelov.coins.ui.fragment.SignUpFragment
import dagger.android.AndroidInjection
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasFragmentInjector

import kotlinx.android.synthetic.main.activity_email_sign_in.*
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class EmailSignInActivity : AbstractActivity(), HasFragmentInjector {
    @Inject internal lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sign_in)
        pager.adapter = TabsAdapter(fragmentManager)
        tab_layout.setupWithViewPager(pager)
        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
    }

    override fun fragmentInjector() = fragmentInjector

    private inner class TabsAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val pages = listOf<Pair<Fragment, String>>(
                Pair(SignInFragment(), getString(R.string.sign_in)),
                Pair(SignUpFragment(), getString(R.string.sign_up))
        )

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