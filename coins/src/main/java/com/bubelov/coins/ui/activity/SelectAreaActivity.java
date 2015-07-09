package com.bubelov.coins.ui.activity;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.SeekBar;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 16:24
 */

public class SelectAreaActivity extends AbstractActivity {
    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 0;

    private static final String CENTER_EXTRA = "center";
    private static final String RADIUS_EXTRA = "radius";

    private static final int DEFAULT_ZOOM = 8;

    private GoogleMap map;

    private Marker areaCenter;

    private Circle areaCircle;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_area);

        Toolbar toolbar = findView(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> {
            saveSelectedArea();
            supportFinishAfterTransition();
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.setOnMarkerDragListener(new OnMarkerDragListener());

        NotificationArea notificationArea = new NotificationAreaProvider(this).get();
        boolean findLocationAndMoveArea = false;

        if (savedInstanceState != null) {
            addArea(savedInstanceState.getParcelable(CENTER_EXTRA), savedInstanceState.getInt(RADIUS_EXTRA));
        } else {
            if (notificationArea == null) {
                notificationArea = new NotificationArea(new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE));
                findLocationAndMoveArea = true;
            }

            addArea(notificationArea.getCenter(), notificationArea.getRadiusMeters());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(notificationArea.getCenter(), DEFAULT_ZOOM));
        }

        if (findLocationAndMoveArea) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new LocationApiConnectionCallbacks())
                    .addOnConnectionFailedListener(new LocationAliConnectionFailedListener())
                    .build();

            googleApiClient.connect();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        View bottomPanel = findView(R.id.bottom_panel);
        bottomPanel.post(() -> map.setPadding(0, 0, 0, bottomPanel.getHeight()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == RESULT_OK) {
            findLocation();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (areaCenter != null && areaCircle != null) {
            outState.putParcelable(CENTER_EXTRA, areaCenter.getPosition());
            outState.putInt(RADIUS_EXTRA, (int) areaCircle.getRadius());
        }

        super.onSaveInstanceState(outState);
    }

    private void findLocation() {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (lastLocation != null) {
            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            moveArea(latLng);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
        } else {
            LocationRequest locationRequest = LocationRequest.create().setNumUpdates(1);
            LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
            PendingResult<LocationSettingsResult> locationSettingsResult = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest);

            locationSettingsResult.setResultCallback(result -> {
                switch (result.getStatus().getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, location -> {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            moveArea(latLng);
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        });

                        break;

                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            result.getStatus().startResolutionForResult(SelectAreaActivity.this, REQUEST_CHECK_LOCATION_SETTINGS);
                        } catch (IntentSender.SendIntentException exception) {
                            // Ignoring
                        }

                        break;
                }
            });
        }
    }

    private void addArea(LatLng center, int radius) {
        BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker);
        areaCenter = map.addMarker(new MarkerOptions()
                .position(center)
                .icon(markerDescriptor)
                .anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V)
                .draggable(true));

        CircleOptions circleOptions = new CircleOptions()
                .center(areaCenter.getPosition())
                .radius(radius)
                .fillColor(getResources().getColor(R.color.notification_area))
                .strokeColor(getResources().getColor(R.color.notification_area_border))
                .strokeWidth(4);

        areaCircle = map.addCircle(circleOptions);

        SeekBar seekBar = findView(R.id.seek_bar_radius);
        seekBar.setMax(500000);
        seekBar.setProgress((int) areaCircle.getRadius());
        seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
    }

    private void moveArea(LatLng location) {
        areaCenter.setPosition(location);
        areaCircle.setCenter(location);
    }

    private void saveSelectedArea() {
        NotificationAreaProvider provider = new NotificationAreaProvider(this);
        provider.save(new NotificationArea(areaCircle.getCenter(), (int)areaCircle.getRadius()));
    }

    private class OnMarkerDragListener implements GoogleMap.OnMarkerDragListener {
        @Override
        public void onMarkerDragStart(Marker marker) {
            areaCircle.setFillColor(getResources().getColor(android.R.color.transparent));
        }

        @Override
        public void onMarkerDrag(Marker marker) {
            areaCircle.setCenter(marker.getPosition());
            map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            areaCircle.setFillColor(getResources().getColor(R.color.notification_area));
        }
    }

    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            areaCircle.setRadius(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    private class LocationApiConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            findLocation();
        }

        @Override
        public void onConnectionSuspended(int cause) {
            showToast("Connection to location API was suspended");
        }
    }

    private class LocationAliConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            showToast("Couldn't connect to location API");
        }
    }
}
