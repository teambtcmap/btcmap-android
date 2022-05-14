package search

import android.os.Bundle
import android.text.Editable
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.btcmap.databinding.FragmentPlacesSearchBinding
import location.Location
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlacesSearchFragment : Fragment() {

    private val model: PlacesSearchModel by viewModel()

    private val resultModel: PlacesSearchResultViewModel by sharedViewModel()

    private var _binding: FragmentPlacesSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlacesSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = PlacesSearchFragmentArgs.fromBundle(requireArguments())
        model.setLocation(Location(args.lat.toDouble(), args.lon.toDouble()))

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.list.layoutManager = LinearLayoutManager(requireContext())

        val adapter = PlacesSearchResultsAdapter {
            resultModel.setPlace(it.place)
            findNavController().popBackStack()
        }

        binding.list.adapter = adapter

        model.searchResults
            .onEach { adapter.swapItems(it) }
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

        binding.query.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                model.setSearchString(s.toString())
                binding.clear.visibility = if (TextUtils.isEmpty(s)) View.GONE else View.VISIBLE
            }
        })

        binding.clear.setOnClickListener { binding.query.setText("") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract class TextWatcherAdapter : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {}
    }
}