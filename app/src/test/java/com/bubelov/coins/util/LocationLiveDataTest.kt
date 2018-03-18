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

package com.bubelov.coins.util

import android.Manifest
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.location.Location
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`

class LocationLiveDataTest {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var context: Context
    @Mock private lateinit var locationManager: LocationManager
    @Mock private lateinit var permissionChecker: PermissionChecker
    private lateinit var locationLiveData: LocationLiveData

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(context.getSystemService(Context.LOCATION_SERVICE))
            .thenReturn(locationManager)
    }

    @Test
    fun returnsNullWhenLacksPermissions() {
        `when`(permissionChecker.check(Manifest.permission.ACCESS_FINE_LOCATION))
            .thenReturn(PermissionChecker.CheckResult.DENIED)

        locationLiveData = LocationLiveData(context, permissionChecker)

        assertNull(locationLiveData.blockingObserve())
        verify(permissionChecker, times(2)).check(Manifest.permission.ACCESS_FINE_LOCATION)
        verifyZeroInteractions(permissionChecker)
        verifyZeroInteractions(locationManager)
    }

    @Test
    fun queriesLocation() {
        `when`(permissionChecker.check(Manifest.permission.ACCESS_FINE_LOCATION))
            .thenReturn(PermissionChecker.CheckResult.GRANTED)

        val location = mock(Location::class.java)
        `when`(location.latitude).thenReturn(50.0)
        `when`(location.longitude).thenReturn(0.0)

        `when`(
            locationManager.requestLocationUpdates(
                com.bubelov.coins.eq(LocationManager.GPS_PROVIDER),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyFloat(),
                any(LocationListener::class.java)
            )
        ).thenAnswer({ invocation ->
            val locationListener = invocation.arguments[3] as LocationListener
            locationListener.onLocationChanged(location)
            null
        })

        locationLiveData = LocationLiveData(context, permissionChecker)

        assertEquals(location.latitude, locationLiveData.blockingObserve().latitude, 0.01)
        assertEquals(location.longitude, locationLiveData.blockingObserve().longitude, 0.01)
    }
}