package org.btcmap.place

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import org.btcmap.databinding.PlacePhotoViewerBinding

/** A fullscreen, swipeable viewer for a place's photos. */
class PlacePhotoViewerDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "place-photo-viewer"

        private const val ARG_URLS = "urls"
        private const val ARG_INDEX = "index"

        fun newInstance(urls: ArrayList<String>, index: Int): PlacePhotoViewerDialogFragment {
            return PlacePhotoViewerDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_URLS, urls)
                    putInt(ARG_INDEX, index)
                }
            }
        }
    }

    private var _binding: PlacePhotoViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PlacePhotoViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val urls = requireArguments().getStringArrayList(ARG_URLS).orEmpty()
        val index = requireArguments().getInt(ARG_INDEX)

        binding.pager.adapter = PlacePhotoPagerAdapter(urls)
        binding.pager.setCurrentItem(index, false)
        binding.close.setOnClickListener { dismiss() }

        // The fullscreen theme draws behind the system bars; inset the content
        // so the close button stays reachable and the photo is not clipped.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
