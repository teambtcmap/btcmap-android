package com.bubelov.coins.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.ui.fragment.ConfirmationDialog;
import com.bubelov.coins.ui.fragment.ConfirmationDialogListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 16:24
 */

public class SelectAreaActivity extends AbstractActivity implements ConfirmationDialogListener {
    private static final String CENTER_EXTRA = "center";
    private static final String RADIUS_EXTRA = "radius";

    private static final String SAVE_DATA_DIALOG = "save_data_dialog";

    private GoogleMap map;

    private UserNotificationManager notificationManager;

    private Marker center;
    private Circle circle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_area);

        Toolbar toolbar = findView(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.getUiSettings().setZoomControlsEnabled(false);
        map.setOnMarkerDragListener(new OnMarkerDragListener());

        notificationManager = new UserNotificationManager(this);

        if (savedInstanceState != null) {
            addArea(savedInstanceState.getParcelable(CENTER_EXTRA), savedInstanceState.getInt(RADIUS_EXTRA));
        } else {
            LatLng center = notificationManager.getNotificationAreaCenter();

            if (center == null) {
                center = new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE);
                findMyLocationAndMoveAreaHere();
            }

            addArea(center, notificationManager.getNotificationAreaRadius());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 8));
        }

        SeekBar seekBar = (SeekBar)findViewById(R.id.seek_bar_radius);
        seekBar.setMax(500000);
        seekBar.setProgress((int) circle.getRadius());
        seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_select_area, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_done) {
            saveSelectedArea();
            supportFinishAfterTransition();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (center != null && circle != null) {
            outState.putParcelable(CENTER_EXTRA, center.getPosition());
            outState.putInt(RADIUS_EXTRA, (int) circle.getRadius());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (notificationManager.getNotificationAreaCenter() == null) {
            ConfirmationDialog.newInstance(R.string.app_name).show(getFragmentManager(), SAVE_DATA_DIALOG);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfirmed(String tag) {
        if (SAVE_DATA_DIALOG.equals(tag)) {
            saveSelectedArea();
            supportFinishAfterTransition();
        }
    }

    @Override
    public void onCancelled(String tag) {
        if (SAVE_DATA_DIALOG.equals(tag)) {
            supportFinishAfterTransition();
        }
    }

    private void findMyLocationAndMoveAreaHere() {
        map.setOnMyLocationChangeListener(location -> {
            map.setOnMyLocationChangeListener(null);
            moveArea(new LatLng(location.getLatitude(), location.getLongitude()));
        });
    }

    private void addArea(LatLng center, int radius) {
        this.center = map.addMarker(new MarkerOptions().position(center).draggable(true));

        CircleOptions circleOptions = new CircleOptions()
                .center(this.center.getPosition())
                .radius(getIntent().getIntExtra(RADIUS_EXTRA, notificationManager.getNotificationAreaRadius()))
                .fillColor(getResources().getColor(R.color.notification_area))
                .strokeColor(getResources().getColor(R.color.notification_area_border))
                .strokeWidth(4);

        circle = map.addCircle(circleOptions);
    }

    private void moveArea(LatLng location) {
        center.setPosition(location);
        circle.setCenter(location);
    }

    private void saveSelectedArea() {
        notificationManager.setNotificationAreaCenter(circle.getCenter());
        notificationManager.setNotificationAreaRadius((int) circle.getRadius());
    }

    private class OnMarkerDragListener implements GoogleMap.OnMarkerDragListener {
        @Override
        public void onMarkerDragStart(Marker marker) {
            circle.setFillColor(getResources().getColor(android.R.color.transparent));
        }

        @Override
        public void onMarkerDrag(Marker marker) {
            circle.setCenter(marker.getPosition());
            map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            circle.setFillColor(getResources().getColor(R.color.notification_area));
        }
    }

    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            circle.setRadius(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
