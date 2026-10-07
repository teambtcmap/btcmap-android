package org.btcmap.nav

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.btcmap.i18n.Strings
import org.btcmap.place.MAX_REPORT_PHOTOS
import org.btcmap.util.createPhotoCaptureTarget
import org.btcmap.util.encodePhoto
import org.btcmap.util.rethrowIfCancellation
import java.io.File
import kotlin.coroutines.resume

/**
 * The Android photo picker behind [org.btcmap.ui.AppPlatform.pickPhotos]: asks
 * whether to take or choose, then suspends until the encoded photos are ready.
 *
 * The activity-result launchers must be registered before the fragment is
 * STARTED, so [AppRootFragment] builds this in `onCreate` and hands
 * [pick] to the platform implementation.
 */
class AndroidPhotoPicker(private val fragment: Fragment) {

    private val strings = Strings.current()

    /** The continuation awaiting the user's next photo pick, if any. */
    private var continuation: CancellableContinuation<List<ByteArray>>? = null

    private var pendingCameraUri: Uri? = null

    private var pendingCameraFile: File? = null

    private val pickMedia = fragment.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_REPORT_PHOTOS),
    ) { uris -> finishPick(uris) }

    private val capturePhoto = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        pendingCameraUri = null
        pendingCameraFile = null
        if (success && uri != null) {
            finishPick(listOf(uri), file)
        } else {
            file?.delete()
            resume(emptyList())
        }
    }

    /** Suspends until the user has taken or chosen photos, or cancelled. */
    suspend fun pick(): List<ByteArray> = suspendCancellableCoroutine { cont ->
        continuation = cont
        cont.invokeOnCancellation { if (continuation === cont) continuation = null }
        showAddPhotoDialog()
    }

    private fun showAddPhotoDialog() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(strings["add_photo"])
            .setItems(
                arrayOf(
                    strings["report_photo_take"],
                    strings["report_photo_choose"],
                ),
            ) { _, which ->
                when (which) {
                    0 -> startCamera()
                    1 -> pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            }
            .setOnCancelListener { resume(emptyList()) }
            .show()
    }

    private fun startCamera() {
        val (uri, file) = fragment.createPhotoCaptureTarget()

        pendingCameraUri = uri
        pendingCameraFile = file

        try {
            capturePhoto.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingCameraUri = null
            pendingCameraFile = null
            file.delete()
            fragment.toast(strings["report_photo_no_camera"])
            resume(emptyList())
        }
    }

    /**
     * Reads and re-encodes the picked images, then resumes with whichever
     * decoded successfully. [cleanupFile] is the temporary capture backing a
     * camera result; it is removed once the photo has been read.
     */
    private fun finishPick(uris: List<Uri>, cleanupFile: File? = null) {
        val appContext = fragment.context?.applicationContext
        if (appContext == null) {
            cleanupFile?.delete()
            resume(emptyList())
            return
        }

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                var failed = false
                val encoded = uris.mapNotNull { uri ->
                    try {
                        appContext.encodePhoto(uri)
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        failed = true
                        null
                    }
                }
                if (failed) fragment.toast(strings["report_photo_failed"])
                resume(encoded)
            } finally {
                cleanupFile?.delete()
            }
        }
    }

    private fun resume(photos: List<ByteArray>) {
        val pending = continuation
        continuation = null
        if (pending?.isActive == true) pending.resume(photos)
    }
}

private fun Fragment.toast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}
