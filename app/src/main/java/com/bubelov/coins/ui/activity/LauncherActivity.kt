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

package com.bubelov.coins.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import timber.log.Timber

class LauncherActivity : AppCompatActivity() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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