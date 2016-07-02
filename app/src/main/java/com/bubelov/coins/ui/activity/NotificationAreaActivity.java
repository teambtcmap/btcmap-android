package com.bubelov.coins.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.util.OnSeekBarChangeAdapter;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 16:24
 */

public class NotificationAreaActivity extends AbstractActivity implements OnMapReadyCallback {
    private static final NotificationArea DEFAULT_NOTIFICATION_AREA = new NotificationArea(Constants.DEFAULT_LOCATION);

    private static final int DEFAULT_ZOOM = 8;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.bottom_panel)
    View bottomPanel;

    @BindView(R.id.seek_bar_radius)
    SeekBar radiusSeekBar;

    private GoogleMap map;

    private Circle areaCircle;

    public static void start(Activity activity) {
        Intent intent = new Intent(activity, NotificationAreaActivity.class);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Notification area screen")
                .putContentType("Screens")
                .putContentId("Notification area"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_area);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
            saveArea();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.setOnMarkerDragListener(new OnMarkerDragListener());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        NotificationArea notificationArea = new NotificationAreaProvider(this).get();
        setArea(notificationArea == null ? DEFAULT_NOTIFICATION_AREA : notificationArea);
    }

    private void setArea(NotificationArea area) {
        BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker);

        Marker areaCenter = map.addMarker(new MarkerOptions()
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

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(area.getCenter(), DEFAULT_ZOOM));
    }

    private void saveArea() {
        NotificationAreaProvider provider = new NotificationAreaProvider(this);
        provider.save(new NotificationArea(areaCircle.getCenter(), (int) areaCircle.getRadius()));
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

    private class SeekBarChangeListener extends OnSeekBarChangeAdapter {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            areaCircle.setRadius(progress);
        }
    }
}