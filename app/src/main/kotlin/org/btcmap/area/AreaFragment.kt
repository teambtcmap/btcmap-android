package org.btcmap.area

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api
import org.btcmap.databinding.AreaFragmentBinding

class AreaFragment : Fragment() {

    private val areaId by lazy {
        requireArguments().getString("area_id")!!
    }

    private var _binding: AreaFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AreaFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        initInsets(binding.root)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val area = withContext(Dispatchers.IO) {
                    api().getArea(areaId)
                }
                binding.toolbar.title = area.name
                binding.icon.isVisible = area.icon != null
                binding.icon.load(area.iconWide ?: area.icon)
                binding.description.isVisible = area.description != null
                binding.description.text = area.description
                binding.website.text = area.websiteUrl
                    .replace("https://", "")
                    .replace("http://", "")
                    .trimEnd('/')
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
    }
}