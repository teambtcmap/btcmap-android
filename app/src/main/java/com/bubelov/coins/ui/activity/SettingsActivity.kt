package com.bubelov.coins.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle

import com.bubelov.coins.R

import kotlinx.android.synthetic.main.activity_settings.*

/**
 * @author Igor Bubelov
 */

class SettingsActivity : AbstractActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}