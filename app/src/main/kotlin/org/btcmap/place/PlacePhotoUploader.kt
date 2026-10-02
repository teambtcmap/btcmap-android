package org.btcmap.place

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.addPlaceImage
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs
import org.btcmap.util.createPhotoCaptureTarget
import org.btcmap.util.encodePhoto
import org.btcmap.util.rethrowIfCancellation
import java.io.File

/**
 * The add-photo flow shared by the standalone place screen and the map's place
 * sheet: the take/choose source dialog, the camera and picker launchers, the
 * upload and the progress dialog.
 *
 * The two hosts differ only in how they gate on a signed-in user and what they
 * refresh once the photo is uploaded, which [onAuthRequired] and [onUploaded]
 * cover; failures go to [onError]. Construct it as a fragment property so the
 * launchers are registered before the fragment is created.
 */
class PlacePhotoUploader(
    private val fragment: Fragment,
    private val onAuthRequired: (placeId: Long, placeName: String) -> Unit,
    private val onUploaded: (placeId: Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) {

    private var placeId = 0L
    private var pendingUri: Uri? = null
    private var pendingFile: File? = null
    private var uploadingDialog: AlertDialog? = null

    private val pickPhoto = fragment.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) uploadPhoto(uri)
    }

    private val capturePhoto = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingUri
        val file = pendingFile
        pendingUri = null
        pendingFile = null
        if (success && uri != null) uploadPhoto(uri, file) else file?.delete()
    }

    /** Starts the flow for a place, prompting for auth first if needed. */
    fun request(placeId: Long, placeName: String) {
        this.placeId = placeId
        if (prefs.authorized) showSourceDialog() else onAuthRequired(placeId, placeName)
    }

    /** Continues the flow after the auth form came back, if it did. */
    fun resumeAfterAuth(placeId: Long) {
        if (placeId <= 0L) return
        this.placeId = placeId
        showSourceDialog()
    }

    /** Dismisses the upload progress dialog; call from `onDestroyView`. */
    fun hideUploading() {
        uploadingDialog?.dismiss()
        uploadingDialog = null
    }

    private fun showSourceDialog() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.add_photo)
            .setItems(
                arrayOf(
                    fragment.getString(R.string.report_photo_take),
                    fragment.getString(R.string.report_photo_choose),
                ),
            ) { _, which ->
                when (which) {
                    0 -> startCamera()
                    1 -> pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            }
            .show()
    }

    private fun startCamera() {
        val (uri, file) = fragment.createPhotoCaptureTarget()
        pendingUri = uri
        pendingFile = file

        try {
            capturePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingUri = null
            pendingFile = null
            file.delete()
            Toast.makeText(fragment.requireContext(), R.string.report_photo_no_camera, Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Reads and re-encodes the picked photo, uploads it to [placeId] and tells
     * the host to refresh. [cleanupFile] is the temporary capture backing a
     * camera result; it is removed once the photo has been read.
     */
    private fun uploadPhoto(uri: Uri, cleanupFile: File? = null) {
        val placeId = placeId
        if (placeId <= 0L) {
            cleanupFile?.delete()
            return
        }

        val context = fragment.requireContext().applicationContext
        showUploading()

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val photo = withContext(Dispatchers.IO) { context.encodePhoto(uri) }
                fragment.api().addPlaceImage(placeId, photo)
                fragment.viewLifecycleOwner.lifecycle.withResumed {
                    hideUploading()
                    onUploaded(placeId)
                }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                fragment.viewLifecycleOwner.lifecycle.withResumed {
                    hideUploading()
                    onError(t)
                }
            } finally {
                cleanupFile?.delete()
            }
        }
    }

    private fun showUploading() {
        if (uploadingDialog?.isShowing == true) return

        val view = fragment.layoutInflater.inflate(R.layout.account_progress_dialog, null)
        view.findViewById<TextView>(R.id.progressMessage).text = fragment.getString(R.string.loading)
        uploadingDialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(view)
            .setCancelable(false)
            .show()
    }
}
