package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.loader.MerchantsLoader;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.ui.fragment.CurrenciesFilterDialog;
import com.bubelov.coins.ui.widget.DrawerMenu;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.Collection;

public class MapActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<Cursor>, DrawerMenu.OnMenuItemSelectedListener {
    private static final int MERCHANTS_LOADER = 0;

    private DrawerLayout drawer;

    private ActionBarDrawerToggle drawerToggle;

    private GoogleMap map;

    private ClusterManager<Merchant> merchantsManager;

    public static Intent newShowMerchantIntent(Context context, double latitude, double longitude) {
        return new Intent(context, MapActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new DrawerToggle(this, drawer, android.R.string.ok, android.R.string.ok);
        drawer.setDrawerListener(drawerToggle);

        DrawerMenu drawerMenu = (DrawerMenu) findViewById(R.id.left_drawer);
        drawerMenu.setItemSelectedListener(this);
        drawerMenu.setSelected(R.id.all);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMyLocationEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(false);

        initClustering();

        map.setOnCameraChangeListener(new OnCameraChangeMultiplexer(merchantsManager, new CameraChangeListener()));

        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        getSupportLoaderManager().initLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds), this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        switch (id) {
            case R.id.action_filter:
                new CurrenciesFilterDialog(this).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == MERCHANTS_LOADER) {
            return new MerchantsLoader(this, args);
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

        merchantsManager.clearItems();
        merchantsManager.addItems(merchants);
        merchantsManager.cluster();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        merchantsManager.clearItems();
        merchantsManager.cluster();
    }

    @Override
    public void onMenuItemSelected(int id, com.bubelov.coins.ui.widget.MenuItem menuItem) {
        drawer.closeDrawer(Gravity.LEFT);

        if (id != R.id.settings && id != R.id.help && id != R.id.donate) {
            getSupportActionBar().setTitle(menuItem.getText());
        }

        if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
        }
    }

    private void initClustering() {
        merchantsManager = new ClusterManager<>(this, map);
        merchantsManager.setRenderer(new PlacesRenderer(this, map, merchantsManager));

        map.setOnCameraChangeListener(merchantsManager);
        map.setOnMarkerClickListener(merchantsManager);
    }

    private class DrawerToggle extends ActionBarDrawerToggle {
        public DrawerToggle(Activity activity, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            invalidateOptionsMenu();
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            invalidateOptionsMenu();
        }
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
            getSupportLoaderManager().restartLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds), MapActivity.this);
        }
    }
}