package com.bubelov.coins.ui.activity

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.bubelov.coins.R
import dagger.android.AndroidInjection
import dagger.android.HasFragmentInjector

import kotlinx.android.synthetic.main.activity_settings.*
import dagger.android.DispatchingAndroidInjector
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class SettingsActivity : AppCompatActivity(), HasFragmentInjector {
    @Inject internal lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
    }

    override fun fragmentInjector() = fragmentInjector

    companion object {
        fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}