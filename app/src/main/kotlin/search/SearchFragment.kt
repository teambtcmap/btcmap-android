package search

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.btcmap.databinding.FragmentSearchBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.util.GeoPoint

class SearchFragment : Fragment() {

    private val model: SearchModel by viewModel()

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = SearchFragmentArgs.fromBundle(requireArguments())

        model.setLocation(GeoPoint(args.lat.toDouble(), args.lon.toDouble()))

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.list.layoutManager = LinearLayoutManager(requireContext())

        val adapter = SearchAdapter { row ->
            findNavController().navigate(
                SearchFragmentDirections.actionSearchFragmentToElementFragment(
                    row.element.id,
                ),
            )
        }

        binding.list.adapter = adapter

        model.searchResults
            .onEach { adapter.submitList(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.query.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = requireActivity().getSystemService(InputMethodManager::class.java)
                imm.showSoftInput(binding.query, InputMethodManager.SHOW_IMPLICIT)
            } else {
                val imm = requireActivity().getSystemService(InputMethodManager::class.java)
                imm.showSoftInput(binding.query, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }
        }

        binding.query.requestFocus()

        binding.query.doAfterTextChanged {
            val text = it.toString()
            model.setSearchString(text)
            binding.clear.isVisible = text.isNotEmpty()
        }

        binding.clear.setOnClickListener { binding.query.setText("") }

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}