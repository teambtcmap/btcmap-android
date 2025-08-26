package user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.runBlocking
import org.btcmap.R
import org.btcmap.databinding.FragmentUserBinding
import org.koin.android.ext.android.inject

class UserFragment : Fragment() {

    private data class Args(
        val userId: Long,
    )

    private val args = lazy {
        Args(requireArguments().getLong("user_id"))
    }

    private val usersRepo: UsersRepo by inject()

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val user = runBlocking {
            usersRepo.selectById(args.value.userId)
        } ?: return

        val userName = user.osmData.optString("display_name")

        binding.topAppBar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_view_on_osm) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://www.openstreetmap.org/user/${userName}")
                startActivity(intent)
            }

            true
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}