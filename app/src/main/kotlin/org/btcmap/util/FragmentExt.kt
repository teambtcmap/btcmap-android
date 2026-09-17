package org.btcmap.util

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.btcmap.R

fun Fragment.openInBrowser(uri: Uri) {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            uri,
        )
    )
}

fun Fragment.showError(throwable: Throwable) {
    Toast.makeText(
        requireContext(),
        throwable.userFacingMessage(getString(R.string.error)),
        Toast.LENGTH_LONG,
    ).show()
}
