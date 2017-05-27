package com.bubelov.coins.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.SeekBar;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.repository.area.NotificationAreaRepository;
import com.bubelov.coins.util.OnSeekBarChangeAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class NotificationAreaActivity extends AbstractActivity implements OnMapReadyCallback {
    public static final String DEFAULT_CAMERA_POSITION_EXTRA = "default_camera_position";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.bottom_panel)
    View bottomPanel;

    @BindView(R.id.seek_bar_radius)
    SeekBar radiusSeekBar;

    @Inject
    NotificationAreaRepository notificationAreaRepository;

    private GoogleMap map;

    private Circle areaCircle;

    private CameraPosition defaultCameraPosition;

    public static Intent newIntent(Context context, CameraPosition defaultCameraPosition) {
        Intent intent = new Intent(context, NotificationAreaActivity.class);
        intent.putExtra(DEFAULT_CAMERA_POSITION_EXTRA, defaultCameraPosition);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dependencies().inject(this);
        setContentView(R.layout.activity_select_area);
        ButterKnife.bind(this);

        defaultCameraPosition = getIntent().getParcelableExtra(DEFAULT_CAMERA_POSITION_EXTRA);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        radiusSeekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_IN);
        radiusSeekBar.getThumb().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_IN);

        toolbar.setNavigationOnClickListener(v -> {
            saveArea();
            supportFinishAfterTransition();
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        bottomPanel.post(() -> map.setPadding(0, 0, 0, bottomPanel.getHeight()));
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

        NotificationArea notificationArea = notificationAreaRepository.getNotificationArea();

        if (notificationArea == null) {
            notificationArea = NotificationArea.builder()
                    .latitude(defaultCameraPosition.target.latitude)
                    .longitude(defaultCameraPosition.target.longitude)
                    .radius(Constants.DEFAULT_NOTIFICATION_AREA_RADIUS_METERS)
                    .build();
        }

        setArea(notificationArea);
    }

    private void setArea(NotificationArea area) {
        BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_location);

        Marker marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(area.latitude(), area.longitude()))
                .icon(markerDescriptor)
                .anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V)
                .draggable(true));

        CircleOptions circleOptions = new CircleOptions()
                .center(marker.getPosition())
                .radius(area.radius())
                .fillColor(getResources().getColor(R.color.notification_area))
                .strokeColor(getResources().getColor(R.color.notification_area_border))
                .strokeWidth(4);

        areaCircle = map.addCircle(circleOptions);

        radiusSeekBar.setMax(500000);
        radiusSeekBar.setProgress((int) areaCircle.getRadius());
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());

        LatLng areaCenter = new LatLng(area.latitude(), area.longitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(areaCenter, getZoomLevel(areaCircle) - 1));
    }

    private void saveArea() {
        NotificationArea area = NotificationArea.builder()
                .latitude(areaCircle.getCenter().latitude)
                .longitude(areaCircle.getCenter().longitude)
                .radius(areaCircle.getRadius())
                .build();

        notificationAreaRepository.setNotificationArea(area);
    }

    public int getZoomLevel(@NonNull Circle circle) {
        double radius = circle.getRadius();
        double scale = radius / 500;
        return (int) (16 - Math.log(scale) / Math.log(2));
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