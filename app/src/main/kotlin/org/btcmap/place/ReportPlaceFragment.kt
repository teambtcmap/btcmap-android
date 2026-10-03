package org.btcmap.place

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import coil3.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.ReportPhotoItemBinding
import org.btcmap.databinding.ReportPlaceFragmentBinding
import org.btcmap.util.createPhotoCaptureTarget
import org.btcmap.util.encodePhoto
import org.btcmap.util.rethrowIfCancellation
import java.io.File

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

    private val photos get() = viewModel.photos
    private val removeButtons = mutableListOf<ImageButton>()

    private var submitting = false
    private var encoding = false

    private var _binding: ReportPlaceFragmentBinding? = null
    private val binding get() = _binding!!

    private val pickPhotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_REPORT_PHOTOS),
    ) { uris ->
        if (uris.isNotEmpty()) addPhotos(uris)
    }

    private val capturePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = viewModel.pendingCameraUri
        val file = viewModel.pendingCameraFile
        viewModel.pendingCameraUri = null
        viewModel.pendingCameraFile = null
        if (success && uri != null) addPhotos(listOf(uri), file) else file?.delete()
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

        binding.reportType.setOnCheckedChangeListener { _, checkedId ->
            if (!submitting) binding.btnSubmit.isEnabled = checkedId != View.NO_ID
        }

        when (ReportType.fromValue(args.defaultType)) {
            ReportType.Verified -> binding.reportType.check(R.id.typeVerified)
            ReportType.RefusedSats -> binding.reportType.check(R.id.typeRefusedSats)
            ReportType.OutOfBusiness -> binding.reportType.check(R.id.typeOutOfBusiness)
            null -> Unit
        }

        binding.btnAddPhoto.setOnClickListener { showAddPhotoDialog() }
        binding.btnSubmit.setOnClickListener { submit() }

        renderPhotos()
        setFormEnabled(true)
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
                    1 -> pickPhotos.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            }
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
        }
    }

    /**
     * Reads and re-encodes the picked images, then appends whichever decoded
     * successfully to the report. [cleanupFile] is the temporary capture that
     * backs a camera result; it is removed once the photo has been read.
     */
    private fun addPhotos(uris: List<Uri>, cleanupFile: File? = null) {
        if (_binding == null) {
            cleanupFile?.delete()
            return
        }

        val remaining = MAX_REPORT_PHOTOS - photos.size
        if (remaining <= 0) {
            cleanupFile?.delete()
            showToast(R.string.report_photo_limit)
            return
        }

        val accepted = uris.take(remaining)
        if (accepted.size < uris.size) showToast(R.string.report_photo_limit)

        val context = requireContext().applicationContext
        encoding = true
        updatePhotoControls()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var failed = false
                val encoded = accepted.mapNotNull { uri ->
                    try {
                        context.encodePhoto(uri)
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        failed = true
                        null
                    }
                }

                if (_binding == null) return@launch

                photos += encoded
                renderPhotos()
                if (failed) showToast(R.string.report_photo_failed)
            } finally {
                cleanupFile?.delete()
                encoding = false
                if (_binding != null) updatePhotoControls()
            }
        }
    }

    private fun renderPhotos() {
        val container = binding.photos
        container.removeAllViews()
        removeButtons.clear()

        binding.photosScroll.isVisible = photos.isNotEmpty()

        photos.forEachIndexed { index, bytes ->
            val item = ReportPhotoItemBinding.inflate(layoutInflater, container, false)
            item.photoImage.load(bytes)
            item.btnRemove.apply {
                isEnabled = !submitting
                setOnClickListener {
                    photos.removeAt(index)
                    renderPhotos()
                    updatePhotoControls()
                }
            }
            removeButtons += item.btnRemove
            container.addView(item.root)
        }

        updatePhotoControls()
    }

    private fun setFormEnabled(enabled: Boolean) {
        submitting = !enabled
        binding.reportType.isEnabled = enabled
        binding.comment.isEnabled = enabled
        binding.btnSubmit.isEnabled = enabled && binding.reportType.checkedRadioButtonId != View.NO_ID
        updatePhotoControls()
    }

    private fun updatePhotoControls() {
        if (_binding == null) return
        binding.btnAddPhoto.isEnabled = !submitting && !encoding && photos.size < MAX_REPORT_PHOTOS
        removeButtons.forEach { it.isEnabled = !submitting }
    }

    private fun submit() {
        val type = when (binding.reportType.checkedRadioButtonId) {
            R.id.typeVerified -> ReportType.Verified
            R.id.typeRefusedSats -> ReportType.RefusedSats
            R.id.typeOutOfBusiness -> ReportType.OutOfBusiness
            else -> return
        }

        val note = binding.comment.text?.toString().orEmpty()

        setFormEnabled(false)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().submitReport(
                    placeId = args.placeId,
                    draft = ReportDraft(type = type, note = note, photos = photos.toList()),
                )
                withResumed {
                    Toast.makeText(
                        requireContext(),
                        R.string.report_submitted,
                        Toast.LENGTH_LONG,
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                withResumed {
                    setFormEnabled(true)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.error)
                        .setMessage(t.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun showToast(@StringRes message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
