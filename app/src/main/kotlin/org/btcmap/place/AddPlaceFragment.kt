package org.btcmap.place

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.submitPlace
import org.btcmap.databinding.AddPlaceFragmentBinding
import org.btcmap.settings.mapStyle
import org.btcmap.settings.markerBackgroundColor
import org.btcmap.settings.prefs
import org.btcmap.settings.uri
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.setFieldError
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class AddPlaceFragment : Fragment() {

    private data class Args(
        val lat: Double,
        val lon: Double,
    )

    private val args by lazy {
        Args(
            lat = requireArguments().getDouble("lat"),
            lon = requireArguments().getDouble("lon"),
        )
    }

    private var _binding: AddPlaceFragmentBinding? = null
    private val binding get() = _binding!!

    private var map: MapLibreMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AddPlaceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The MapView owns native resources and must receive its lifecycle
        // callbacks, like MapFragment's map; without onCreate and onDestroy in
        // particular, reopening this screen leaves a dead renderer behind and
        // later queries can crash natively.
        binding.map.onCreate(savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.zoomIn.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.zoomOut.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.map.getMapAsync { map ->
            this.map = map
            map.setStyle(
                Style.Builder().fromUri(prefs.mapStyle.uri(requireContext()))
            )
            map.uiSettings.setAllGesturesEnabled(true)
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isCompassEnabled = false

            val markerColor = prefs.markerBackgroundColor(requireContext())
            map.getStyle { style ->
                if (style.getImage("btcmap-marker") == null) {
                    val drawable = androidx.appcompat.content.res.AppCompatResources
                        .getDrawable(requireContext(), R.drawable.map_marker)!!
                        .mutate()
                    androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, markerColor)
                    style.addImage("btcmap-marker", drawable)
                }
            }

            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(args.lat, args.lon), 16.0,
                )
            )

            map.addOnCameraIdleListener(object : MapLibreMap.OnCameraIdleListener {
                override fun onCameraIdle() {
                    val center = map.cameraPosition.target ?: return
                    updatePinPosition(center)
                }
            })
        }

        binding.btnSubmit.setOnClickListener { submit() }
    }

    private var currentLatLng: LatLng? = null

    private fun updatePinPosition(target: LatLng) {
        currentLatLng = target
    }

    private fun submit() {
        val name = binding.name.text?.toString()?.trim().orEmpty()
        val category = binding.category.text?.toString()?.trim().orEmpty()
        val address = binding.address.text?.toString()?.trim().orEmpty()
        val website = binding.website.text?.toString()?.trim().orEmpty()
        val description = binding.description.text?.toString()?.trim().orEmpty()

        var valid = true
        // Clear errors from the previous attempt first, so a corrected field
        // does not keep showing an error that no longer applies.
        binding.name.setFieldError(null)
        binding.category.setFieldError(null)
        binding.address.setFieldError(null)
        if (name.isEmpty()) {
            binding.name.setFieldError(getString(R.string.field_required))
            valid = false
        }
        if (category.isEmpty()) {
            binding.category.setFieldError(getString(R.string.field_required))
            valid = false
        }
        if (address.isEmpty()) {
            binding.address.setFieldError(getString(R.string.field_required))
            valid = false
        }
        if (!valid) return

        binding.btnSubmit.isEnabled = false
        binding.name.isEnabled = false
        binding.category.isEnabled = false
        binding.address.isEnabled = false
        binding.website.isEnabled = false
        binding.description.isEnabled = false

        val lat = currentLatLng?.latitude ?: args.lat
        val lon = currentLatLng?.longitude ?: args.lon

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().submitPlace(
                    lat = lat,
                    lon = lon,
                    category = category,
                    name = name,
                    address = address.takeIf { it.isNotEmpty() },
                    website = website.takeIf { it.isNotEmpty() },
                    description = description.takeIf { it.isNotEmpty() },
                )
                withResumed {
                    Toast.makeText(
                        requireContext(),
                        R.string.place_submitted,
                        Toast.LENGTH_LONG,
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                withResumed {
                    binding.btnSubmit.isEnabled = true
                    binding.name.isEnabled = true
                    binding.category.isEnabled = true
                    binding.address.isEnabled = true
                    binding.website.isEnabled = true
                    binding.description.isEnabled = true
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.error)
                        .setMessage(t.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        _binding?.map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        _binding?.map?.onResume()
    }

    override fun onPause() {
        _binding?.map?.onPause()
        super.onPause()
    }

    override fun onStop() {
        _binding?.map?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.map?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.map?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.map?.onDestroy()
        _binding = null
        map = null
    }
}
