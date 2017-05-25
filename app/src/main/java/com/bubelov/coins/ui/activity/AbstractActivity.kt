package com.bubelov.coins.ui.activity

import android.arch.lifecycle.LifecycleActivity
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog

import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.dagger.MainComponent
import com.bubelov.coins.ui.dialog.ProgressDialog
import com.bubelov.coins.util.ThemeUtils

/**
 * @author Igor Bubelov
 */

abstract class AbstractActivity : LifecycleActivity() {
    var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.ensureRuntimeTheme(this)
        super.onCreate(savedInstanceState)
    }

    fun showAlert(@StringRes messageResId: Int) {
        showAlert(getString(messageResId))
    }

    fun showAlert(message: String) {
        AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    protected fun showProgress() {
        hideProgress()
        progressDialog = ProgressDialog.show(this, "", "", true, false)
    }

    protected fun hideProgress() {
        if (progressDialog != null) {
            progressDialog!!.hide()
        }
    }

    protected fun dependencies(): MainComponent {
        return Injector.INSTANCE.mainComponent()
    }
}