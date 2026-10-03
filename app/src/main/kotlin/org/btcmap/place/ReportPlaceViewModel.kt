package org.btcmap.place

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.io.File

/**
 * Holds the evidence photos attached to a report together with the temporary
 * file backing an in-flight camera capture. Keeping them here means the
 * attached photos survive a configuration change and a camera result can still
 * be matched to its file after the screen is rebuilt.
 *
 * The photos are a Compose `SnapshotStateList` because the shared report screen
 * reads and mutates them directly.
 */
internal class ReportPlaceViewModel : ViewModel() {
    val photos = mutableStateListOf<ByteArray>()

    var pendingCameraUri: Uri? = null
    var pendingCameraFile: File? = null

    override fun onCleared() {
        pendingCameraFile?.delete()
    }
}
