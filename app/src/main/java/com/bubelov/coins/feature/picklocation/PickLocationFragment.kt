/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.feature.picklocation

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.bubelov.coins.R
import com.bubelov.coins.model.Location
import com.bubelov.coins.util.toLatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_pick_location.*

class PickLocationFragment : DaggerFragment() {
    private val map = MutableLiveData<GoogleMap>()

    private val initialLocation = Location(0.0, 0.0)

    private val initialZoom = 15f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pick_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.apply {
            setNavigationOnClickListener { findNavController().popBackStack() }
            inflateMenu(R.menu.pick_location)

            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_done) {
                    // TODO
                    return@setOnMenuItemClickListener true
                }

                false
            }
        }

        (childFragmentManager.findFragmentById(R.id.map) as MapFragment).getMapAsync { map.value = it }

        map.observe(this, Observer { map ->
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation.toLatLng(), initialZoom))
        })
    }
}