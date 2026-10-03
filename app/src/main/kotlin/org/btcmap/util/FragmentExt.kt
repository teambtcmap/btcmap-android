package org.btcmap.util

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import org.btcmap.R
import java.io.File

/**
 * Creates a temporary cache file and a FileProvider Uri the camera app can
 * write a new photo to. The caller owns the file and must delete it once the
 * photo has been read.
 */
fun Fragment.createPhotoCaptureTarget(): Pair<Uri, File> {
    val dir = File(requireContext().cacheDir, "photos").apply { mkdirs() }
    val file = File.createTempFile("photo-", ".jpg", dir)
    val uri = FileProvider.getUriForFile(
        requireContext(),
        "${requireContext().packageName}.fileprovider",
        file,
    )
    return uri to file
}

fun Fragment.openInBrowser(uri: Uri) {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            uri,
        )
    )
}

/** Opens the dialer with [phone] pre-filled. Does nothing when it is blank. */
fun Fragment.openDialer(phone: String?) {
    val number = phone?.trim()
    if (number.isNullOrEmpty()) return
    startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
}

/** Opens the mail app with [email] as the recipient. Does nothing when blank. */
fun Fragment.openEmail(email: String?) {
    val address = email?.trim()
    if (address.isNullOrEmpty()) return
    startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri()))
}

fun Fragment.showError(throwable: Throwable) {
    Toast.makeText(
        requireContext(),
        throwable.userFacingMessage(getString(R.string.error)),
        Toast.LENGTH_LONG,
    ).show()
}
