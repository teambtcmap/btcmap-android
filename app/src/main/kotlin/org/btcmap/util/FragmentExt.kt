package org.btcmap.util

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

fun Fragment.openInBrowser(uri: Uri) {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            uri,
        )
    )
}