package com.bubelov.coins.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.loader.MerchantsLoader;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 12:52
 */

public class MerchantsMapFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int MERCHANTS_LOADER = 0;

    private GoogleMap map;

    private ClusterManager<Merchant> placesManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMyLocationEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);

        findMyLocation();

        initClustering();

        map.setOnCameraChangeListener(new OnCameraChangeMultiplexer(placesManager, new CameraChangeListener()));

        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        getLoaderManager().initLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds), this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == MERCHANTS_LOADER) {
            return new MerchantsLoader(getActivity(), args);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Collection<Merchant> merchants = new ArrayList<>();

        while (data.moveToNext()) {
            Merchant merchant = new Merchant();
            merchant.setId(data.getLong(0));
            merchant.setLatitude(data.getFloat(1));
            merchant.setLongitude(data.getFloat(2));
            merchant.setName(data.getString(3));
            merchant.setDescription(data.getString(4));

            merchants.add(merchant);
        }

        placesManager.clearItems();
        placesManager.addItems(merchants);
        placesManager.cluster();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        placesManager.clearItems();
        placesManager.cluster();
    }

    private void findMyLocation() {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE), 10));

        map.setOnMyLocationChangeListener(location -> {
            map.setOnMyLocationChangeListener(null);
            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLng(myLocation));

            UserNotificationManager notificationManager = new UserNotificationManager(getActivity());

            if (notificationManager.getNotificationAreaCenter() == null) {
                notificationManager.setNotificationAreaCenter(myLocation);
            }
        });
    }

    private void initClustering() {
        placesManager = new ClusterManager<>(getActivity(), map);
        placesManager.setRenderer(new PlacesRenderer(getActivity(), map, placesManager));

        map.setOnCameraChangeListener(placesManager);
        map.setOnMarkerClickListener(placesManager);
    }

    private class PlacesRenderer extends DefaultClusterRenderer<Merchant> {
        public PlacesRenderer(Context context, GoogleMap map, ClusterManager<Merchant> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(Merchant item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);

            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_white_48dp));

            if (!TextUtils.isEmpty(item.getName())) {
                markerOptions.title(item.getName());
            }

            if (!TextUtils.isEmpty(item.getDescription())) {
                markerOptions.snippet(item.getDescription());
            }
        }
    }

    private class CameraChangeListener implements GoogleMap.OnCameraChangeListener {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
            getLoaderManager().restartLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds), MerchantsMapFragment.this);
        }
    }
}
