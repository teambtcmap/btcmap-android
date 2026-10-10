package org.btcmap.util

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
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
