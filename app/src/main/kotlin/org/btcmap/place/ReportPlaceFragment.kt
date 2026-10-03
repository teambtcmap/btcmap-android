package org.btcmap.place

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.ReportPlaceFragmentBinding
import org.btcmap.ui.ReportPlaceLabels
import org.btcmap.util.createPhotoCaptureTarget
import org.btcmap.util.encodePhoto
import org.btcmap.util.rethrowIfCancellation
import java.io.File
import kotlin.coroutines.resume

/**
 * The report screen: a toolbar over the shared
 * [org.btcmap.ui.ReportPlaceScreen], which owns the reasons, the note, the
 * evidence photos and the submit. This fragment supplies the labels, the
 * [androidx.compose.runtime.snapshots.SnapshotStateList] of photos (kept in a
 * [ReportPlaceViewModel] so they survive a configuration change) and the photo
 * picker and submit callbacks.
 */
class ReportPlaceFragment : Fragment() {

    private data class Args(
        val placeId: Long,
        val placeName: String?,
        val defaultType: String?,
    )

    private val args by lazy {
        Args(
            placeId = requireArguments().getLong("place_id"),
            placeName = requireArguments().getString("place_name"),
            defaultType = if (requireArguments().containsKey("default_type")) {
                requireArguments().getString("default_type")
            } else null,
        )
    }

    private val viewModel by lazy { ViewModelProvider(this)[ReportPlaceViewModel::class.java] }

    private var _binding: ReportPlaceFragmentBinding? = null
    private val binding get() = _binding!!

    /** The continuation awaiting the user's next photo pick, if any. */
    private var photoContinuation: CancellableContinuation<List<ByteArray>>? = null

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_REPORT_PHOTOS),
    ) { uris -> finishPhotoPick(uris) }

    private val capturePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = viewModel.pendingCameraUri
        val file = viewModel.pendingCameraFile
        viewModel.pendingCameraUri = null
        viewModel.pendingCameraFile = null
        if (success && uri != null) {
            finishPhotoPick(listOf(uri), file)
        } else {
            file?.delete()
            resumePhotos(emptyList())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ReportPlaceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.topAppBar.title = args.placeName

        binding.reportContent.apply {
            initialType = args.defaultType
            labels = reportLabels()
            photos = viewModel.photos
            pickPhotos = ::pickPhotos
            submit = { draft -> api().submitReport(placeId = args.placeId, draft = draft) }
            onBack = { parentFragmentManager.popBackStack() }
        }
    }

    /**
     * The shared screen's photo picker: asks whether to take or choose, then
     * suspends until the launcher returns the encoded photos.
     */
    private suspend fun pickPhotos(): List<ByteArray> = suspendCancellableCoroutine { cont ->
        photoContinuation = cont
        cont.invokeOnCancellation { if (photoContinuation === cont) photoContinuation = null }
        showAddPhotoDialog()
    }

    private fun showAddPhotoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_photo)
            .setItems(
                arrayOf(
                    getString(R.string.report_photo_take),
                    getString(R.string.report_photo_choose),
                ),
            ) { _, which ->
                when (which) {
                    0 -> startCamera()
                    1 -> pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            }
            .setOnCancelListener { resumePhotos(emptyList()) }
            .show()
    }

    private fun startCamera() {
        val (uri, file) = createPhotoCaptureTarget()

        viewModel.pendingCameraFile = file
        viewModel.pendingCameraUri = uri

        try {
            capturePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.pendingCameraFile = null
            viewModel.pendingCameraUri = null
            file.delete()
            showToast(R.string.report_photo_no_camera)
            resumePhotos(emptyList())
        }
    }

    /**
     * Reads and re-encodes the picked images, then resumes the picker with
     * whichever decoded successfully. [cleanupFile] is the temporary capture
     * that backs a camera result; it is removed once the photo has been read.
     */
    private fun finishPhotoPick(uris: List<Uri>, cleanupFile: File? = null) {
        val appContext = context?.applicationContext
        if (appContext == null) {
            cleanupFile?.delete()
            resumePhotos(emptyList())
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
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
                if (failed) showToast(R.string.report_photo_failed)
                resumePhotos(encoded)
            } finally {
                cleanupFile?.delete()
            }
        }
    }

    private fun resumePhotos(photos: List<ByteArray>) {
        val continuation = photoContinuation
        photoContinuation = null
        if (continuation?.isActive == true) continuation.resume(photos)
    }

    private fun reportLabels(): ReportPlaceLabels = ReportPlaceLabels(
        intro = getString(R.string.verify_or_report_description),
        reasonLabel = { getString(it.labelRes()) },
        reasonDescription = { getString(it.descriptionRes()) },
        noteHint = getString(R.string.report_comment_optional),
        addPhoto = { _, _ -> getString(R.string.add_photo) },
        removePhoto = getString(R.string.delete),
        submit = getString(R.string.btn_submit),
        submitted = getString(R.string.report_submitted),
        backToMap = getString(R.string.back_to_map),
    )

    @StringRes
    private fun ReportType.labelRes(): Int = when (this) {
        ReportType.Verified -> R.string.report_type_verified
        ReportType.RefusedSats -> R.string.report_type_refused_sats
        ReportType.OutOfBusiness -> R.string.report_type_out_of_business
    }

    @StringRes
    private fun ReportType.descriptionRes(): Int = when (this) {
        ReportType.Verified -> R.string.report_type_verified_description
        ReportType.RefusedSats -> R.string.report_type_refused_sats_description
        ReportType.OutOfBusiness -> R.string.report_type_out_of_business_description
    }

    private fun showToast(@StringRes message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
