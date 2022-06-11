package search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.btcmap.databinding.FragmentSearchBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.util.GeoPoint

class SearchFragment : Fragment() {

    private val model: SearchModel by viewModel()

    private val resultModel: SearchResultModel by sharedViewModel()

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
            resultModel.element.update { row.element }
            findNavController().popBackStack()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}