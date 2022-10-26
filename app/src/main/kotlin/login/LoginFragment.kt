package login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import conf.ConfRepo
import http.await
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.btcmap.R
import org.btcmap.databinding.FragmentLoginBinding
import org.koin.android.ext.android.inject

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val conf: ConfRepo by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.root.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.login.setOnClickListener { login() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun login() {
        if (!binding.validate()) {
            return
        }

        binding.progress.isVisible = true

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val credentials = Credentials.basic(
                    binding.username.text.toString(),
                    binding.password.text.toString(),
                )
                val response = OkHttpClient().newCall(
                    Request.Builder()
                        .url("https://api.openstreetmap.org/api/0.6/user/preferences.json")
                        .header("Authorization", credentials).build()
                ).await()

                if (response.isSuccessful) {
                    conf.update {
                        it.copy(
                            osmLogin = binding.username.text.toString(),
                            osmPassword = binding.password.text.toString(),
                        )
                    }

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.logged_in),
                        Toast.LENGTH_SHORT,
                    ).show()
                    findNavController().popBackStack()
                } else {
                    throw Exception()
                }
            }.onFailure {
                binding.progress.isVisible = false
                Toast.makeText(requireContext(), R.string.failed_to_login, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun FragmentLoginBinding.validate(): Boolean {
        usernameLayout.error = when (username.text.toString().length) {
            0 -> getString(R.string.field_is_empty)
            else -> null
        }

        if (binding.password.text.isNullOrEmpty()) {
            binding.passwordLayout.error = getString(R.string.field_is_empty)
        } else {
            binding.passwordLayout.error = null
        }

        return usernameLayout.error == null && passwordLayout.error == null
    }
}