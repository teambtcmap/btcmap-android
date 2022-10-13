package users

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import db.SelectAllUsersAsListItems
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentUsersBinding
import org.koin.android.ext.android.inject
import java.util.regex.Pattern

class UsersFragment : Fragment() {

    private val usersRepo: UsersRepo by inject()

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.list.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.list.layoutManager = LinearLayoutManager(requireContext())
                val adapter = UsersAdapter {
                    findNavController().navigate(UsersFragmentDirections.actionUsersFragmentToUserFragment(it.id))
                }
                binding.list.adapter = adapter

                adapter.submitList(usersRepo.selectAllUsersAsListItems().map {
                    val changes = if (it.user_name == "Bill on Bitcoin Island") {
                        it.changes + 120
                    } else {
                        it.changes
                    }

                    UsersAdapter.Item(
                        id = it.user_id,
                        name = it.user_name ?: getString(R.string.unnamed_user),
                        changes = changes,
                        tipLnurl = it.lnurl(),
                        imgHref = it.user_img_href ?: "",
                    )
                }.sortedByDescending { it.changes })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun SelectAllUsersAsListItems.lnurl(): String {
        val description = user_description ?: ""
        val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(description)
        val matchFound: Boolean = matcher.find()

        return if (matchFound) {
            matcher.group().trim('(', ')')
        } else {
            ""
        }
    }
}