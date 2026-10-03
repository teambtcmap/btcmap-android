package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.UserProfileFragmentBinding
import org.btcmap.db
import org.btcmap.ui.ProfileFormLabels
import org.btcmap.ui.UploadedImagesLabels
import org.btcmap.ui.UserProfileLabels

/**
 * The signed-in account page: a toolbar over the shared
 * [org.btcmap.ui.ProfileScreen], which loads the cached user, renders the saved
 * lists and hosts the inline change-username and change-password forms and the
 * logout. This fragment only supplies the API, database, settings and labels.
 */
class UserProfileFragment : Fragment() {

    private var _binding: UserProfileFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UserProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = binding.userProfileList

        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Uploaded images is Compose state inside the profile rather than a
        // back-stack entry, so back returns to the profile page first.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (content.uploadedImagesOpen) {
                content.uploadedImagesOpen = false
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        // The uploaded-images body carries no title, so the toolbar title names
        // the profile's current sub-screen.
        viewLifecycleOwner.lifecycleScope.launch {
            snapshotFlow { content.uploadedImagesOpen }.collect { open ->
                binding.topAppBar.setTitle(if (open) R.string.uploaded_images else R.string.profile)
            }
        }

        content.api = api()
        content.database = db()
        content.settings = prefs
        content.profileLabels = profileLabels()
        content.formLabels = formLabels()
        content.imagesLabels = imagesLabels()
        content.iconTypeface = org.btcmap.util.iconTypeface
        content.onLoggedOut = { parentFragmentManager.popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun profileLabels(): UserProfileLabels = UserProfileLabels(
        username = getString(R.string.username),
        password = getString(R.string.password),
        savedPlaces = getString(R.string.saved_places),
        savedAreas = getString(R.string.saved_areas),
        noSavedPlaces = getString(R.string.no_saved_places),
        noSavedAreas = getString(R.string.no_saved_areas),
        logOut = getString(R.string.logout),
        editUsername = getString(R.string.change_username),
        editPassword = getString(R.string.change_password),
        delete = getString(R.string.delete),
        uploadedImages = getString(R.string.uploaded_images),
    )

    private fun imagesLabels(): UploadedImagesLabels = UploadedImagesLabels(
        empty = getString(R.string.uploaded_images_empty),
        delete = getString(R.string.delete),
        failed = getString(R.string.uploaded_images_failed),
        retry = getString(R.string.retry),
        unknownPlace = { getString(R.string.uploaded_images_place, it) },
    )

    private fun formLabels(): ProfileFormLabels = ProfileFormLabels(
        passwordMask = getString(R.string.password_mask),
        required = getString(R.string.field_required),
        changeUsernameTitle = getString(R.string.change_username),
        changePasswordTitle = getString(R.string.change_password),
        username = getString(R.string.username),
        currentPassword = getString(R.string.current_password),
        newPassword = getString(R.string.new_password),
        confirmPassword = getString(R.string.confirm_password),
        passwordsDoNotMatch = getString(R.string.passwords_do_not_match),
        passwordTooShort = { getString(R.string.password_min_length, it) },
        save = getString(R.string.save),
        cancel = getString(android.R.string.cancel),
        usernameChanged = getString(R.string.username_changed),
        passwordChanged = getString(R.string.password_changed),
    )
}
