package com.bubelov.coins.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
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

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 16:24
 */

public class SelectAreaActivity extends AbstractActivity {
    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 0;

    private static final int REQUEST_ACCESS_LOCATION = 10;

    private static final String AREA_EXTRA = "area";

    private static final int DEFAULT_ZOOM = 8;

    @Bind(R.id.toolbar) Toolbar toolbar;

    @Bind(R.id.bottom_panel) View bottomPanel;

    @Bind(R.id.seek_bar_radius) SeekBar radiusSeekBar;

    private GoogleMap map;
    private GoogleApiClient googleApiClient;

    private Marker areaCenter;
    private Circle areaCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_area);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.setOnMarkerDragListener(new OnMarkerDragListener());

        NotificationArea notificationArea = new NotificationAreaProvider(this).get();
        boolean findLocationAndMoveArea = false;

        if (savedInstanceState != null && savedInstanceState.containsKey(AREA_EXTRA)) {
            addArea((NotificationArea) savedInstanceState.getSerializable(AREA_EXTRA));
        } else {
            if (notificationArea == null) {
                notificationArea = new NotificationArea(new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE));
                findLocationAndMoveArea = true;
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(notificationArea.getCenter(), DEFAULT_ZOOM));
            }

            addArea(notificationArea);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(notificationArea.getCenter(), DEFAULT_ZOOM));
        }

        if (findLocationAndMoveArea) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initLocation();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_ACCESS_LOCATION);
            }
        }

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Area select")
                .putContentType("Screens")
                .putContentId("Area select"));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        bottomPanel.post(new Runnable() {
            @Override
            public void run() {
                map.setPadding(0, 0, 0, bottomPanel.getHeight());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveSelectedArea();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == RESULT_OK) {
            findLocation();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocation();
                }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (areaCenter != null && areaCircle != null) {
            outState.putSerializable(AREA_EXTRA, new NotificationArea(areaCenter.getPosition(), (int) areaCircle.getRadius()));
        }

        super.onSaveInstanceState(outState);
    }

    private void initLocation() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new LocationApiConnectionCallbacks())
                .addOnConnectionFailedListener(new LocationAliConnectionFailedListener())
                .build();

        googleApiClient.connect();
    }

    private void findLocation() {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (lastLocation != null) {
            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            moveArea(latLng);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
        } else {
            final LocationRequest locationRequest = LocationRequest.create().setNumUpdates(1);
            LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
            PendingResult<LocationSettingsResult> locationSettingsResult = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest);

            locationSettingsResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    switch (result.getStatus().getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    SelectAreaActivity.this.moveArea(latLng);
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                                }
                            });

                            break;

                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                result.getStatus().startResolutionForResult(SelectAreaActivity.this, REQUEST_CHECK_LOCATION_SETTINGS);
                            } catch (IntentSender.SendIntentException ignored) {

                            }

                            break;
                    }
                }
            });
        }
    }

    private void addArea(NotificationArea area) {
        BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker);
        areaCenter = map.addMarker(new MarkerOptions()
                .position(area.getCenter())
                .icon(markerDescriptor)
                .anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V)
                .draggable(true));

        CircleOptions circleOptions = new CircleOptions()
                .center(areaCenter.getPosition())
                .radius(area.getRadiusMeters())
                .fillColor(getResources().getColor(R.color.notification_area))
                .strokeColor(getResources().getColor(R.color.notification_area_border))
                .strokeWidth(4);

        areaCircle = map.addCircle(circleOptions);

        radiusSeekBar.setMax(500000);
        radiusSeekBar.setProgress((int) areaCircle.getRadius());
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
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
