package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.UserProfileFragmentBinding
import org.btcmap.db
import org.btcmap.event.AddEventFragment
import org.btcmap.settings.mapStyle
import org.btcmap.settings.uri
import org.btcmap.ui.MyEventUi
import org.btcmap.ui.MyEventsLabels
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

        // Uploaded images and my events are Compose state inside the profile
        // rather than back-stack entries, so back returns to the profile page
        // first.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                content.uploadedImagesOpen -> content.uploadedImagesOpen = false
                content.myEventsOpen -> content.myEventsOpen = false
                else -> parentFragmentManager.popBackStack()
            }
        }

        // The sub-screen bodies carry no title, so the toolbar title names the
        // profile's current sub-screen.
        viewLifecycleOwner.lifecycleScope.launch {
            snapshotFlow { content.uploadedImagesOpen to content.myEventsOpen }.collect { (images, events) ->
                binding.topAppBar.setTitle(
                    when {
                        images -> R.string.uploaded_images
                        events -> R.string.my_events
                        else -> R.string.profile
                    },
                )
            }
        }

        content.api = api()
        content.database = db()
        content.settings = prefs
        content.profileLabels = profileLabels()
        content.formLabels = formLabels()
        content.imagesLabels = imagesLabels()
        content.eventsLabels = eventsLabels()
        content.mapStyleUrl = prefs.mapStyle.uri(requireContext())
        content.iconTypeface = org.btcmap.util.iconTypeface
        content.onDuplicateEvent = { event -> openDuplicate(event) }
        content.onLoggedOut = { parentFragmentManager.popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Opens the add-event screen pre-filled from a submitted event, so a regular
     * event can be repeated with a new date.
     */
    private fun openDuplicate(event: MyEventUi) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AddEventFragment>(
                R.id.fragmentContainerView,
                null,
                Bundle().apply {
                    putDouble("lat", event.lat)
                    putDouble("lon", event.lon)
                    putString("name", event.name)
                    putString("website", event.website)
                    putString("starts_at", event.startsAtLocal)
                    event.endsAtLocal?.let { putString("ends_at", it) }
                },
            )
            addToBackStack(null)
        }
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
        myEvents = getString(R.string.my_events),
    )

    private fun imagesLabels(): UploadedImagesLabels = UploadedImagesLabels(
        empty = getString(R.string.uploaded_images_empty),
        delete = getString(R.string.delete),
        failed = getString(R.string.uploaded_images_failed),
        retry = getString(R.string.retry),
        unknownPlace = { getString(R.string.uploaded_images_place, it) },
    )

    private fun eventsLabels(): MyEventsLabels = MyEventsLabels(
        empty = getString(R.string.my_events_empty),
        failed = getString(R.string.my_events_failed),
        retry = getString(R.string.retry),
        statusPending = getString(R.string.event_status_pending),
        statusLive = getString(R.string.event_status_live),
        statusRejected = getString(R.string.event_status_rejected),
        revoke = getString(R.string.event_revoke),
        revokeFailed = getString(R.string.event_revoke_failed),
        duplicate = getString(R.string.event_duplicate),
        dateRange = { date, start, end ->
            getString(R.string.event_date_time_range, date, start, end)
        },
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
