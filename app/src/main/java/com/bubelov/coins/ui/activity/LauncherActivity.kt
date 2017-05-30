package com.bubelov.coins.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

class LauncherActivity : AbstractActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val playServicesAvailabilityResult = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (playServicesAvailabilityResult == ConnectionResult.SUCCESS) {
            onPlayServicesAvailable()
        } else {
            if (googleApiAvailability.isUserResolvableError(playServicesAvailabilityResult)) {
                val dialog = googleApiAvailability.getErrorDialog(this, playServicesAvailabilityResult, PLAY_SERVICES_RESOLUTION_REQUEST)
                dialog.setCancelable(false)
                dialog.show()
            } else {
                Timber.e(IllegalStateException("Unresolvable Play Services error"))
                supportFinishAfterTransition()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                onPlayServicesAvailable()
            } else {
                supportFinishAfterTransition()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onPlayServicesAvailable() {
        startActivity(Intent(this, MapActivity::class.java))
    }

    companion object {
        private val PLAY_SERVICES_RESOLUTION_REQUEST = 10
    }
}