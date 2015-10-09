package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;

import com.bubelov.coins.Constants;
import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.event.DatabaseSyncFailedEvent;
import com.bubelov.coins.event.MerchantsSyncFinishedEvent;
import com.bubelov.coins.event.DatabaseSyncStartedEvent;
import com.bubelov.coins.loader.MerchantsLoader;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.service.sync.merchants.MerchantsSyncService;
import com.bubelov.coins.ui.fragment.MerchantsCacheFragment;
import com.bubelov.coins.ui.widget.CurrenciesFilterPopup;
import com.bubelov.coins.ui.widget.DrawerMenu;
import com.bubelov.coins.ui.widget.MerchantDetailsView;
import com.bubelov.coins.util.MapMarkersCache;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.bubelov.coins.util.StaticClusterRenderer;
import com.bubelov.coins.util.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MapActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<Cursor>, DrawerMenu.Listener {
    private static final String KEY_AMENITY = "amenity";

    private static final String MERCHANT_ID_EXTRA = "merchant_id";

    private static final int MERCHANTS_LOADER = 0;

    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 0;

    private Toolbar toolbar;

    private Toolbar merchantToolbar;

    private View merchantTopGradient;

    private DrawerLayout drawer;

    private ActionBarDrawerToggle drawerToggle;

    private GoogleMap map;

    private ClusterManager<Merchant> merchantsManager;

    private Amenity amenity;

    private SlidingUpPanelLayout slidingLayout;

    private MerchantDetailsView merchantDetails;

    private View loader;

    private MapMarkersCache markersCache;

    private GoogleApiClient googleApiClient;

    private Merchant selectedMerchant;

    private boolean databaseSyncing;

    private boolean saveCameraPositionFlag;

    private CameraUpdate cameraBeforeSelection;

    private MerchantsCacheFragment merchantsCacheFragment;

    private FloatingActionButton actionButton;

    private boolean firstLaunch;

    public static Intent newShowMerchantIntent(Context context, long merchantId) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(MERCHANT_ID_EXTRA, merchantId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        toolbar = findView(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        merchantToolbar = findView(R.id.merchant_toolbar);
        merchantToolbar.setNavigationOnClickListener(v -> {
            slidingLayout.collapsePanel();
        });

        merchantTopGradient = findView(R.id.merchant_top_gradient);

        amenity = savedInstanceState == null ? null : (Amenity) savedInstanceState.getSerializable(KEY_AMENITY);

        drawer = findView(R.id.drawer_layout);
        drawerToggle = new DrawerToggle(this, drawer, android.R.string.ok, android.R.string.ok);
        drawer.setDrawerListener(drawerToggle);

        DrawerMenu drawerMenu = findView(R.id.left_drawer);
        drawerMenu.setItemSelectedListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        initClustering();

        map.setOnCameraChangeListener(new OnCameraChangeMultiplexer(merchantsManager, new CameraChangeListener()));

        merchantDetails = findView(R.id.merchant_details);

        loader = findView(R.id.loader);

        markersCache = new MapMarkersCache();

        firstLaunch = savedInstanceState == null;

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new LocationApiConnectionCallbacks())
                .addOnConnectionFailedListener(new LocationAliConnectionFailedListener())
                .build();

        googleApiClient.connect();

        merchantsCacheFragment = (MerchantsCacheFragment) getSupportFragmentManager().findFragmentByTag(MerchantsCacheFragment.TAG);

        if (merchantsCacheFragment == null) {
            merchantsCacheFragment = new MerchantsCacheFragment();
            getSupportFragmentManager().beginTransaction().add(merchantsCacheFragment, MerchantsCacheFragment.TAG);
        }

        actionButton = findView(R.id.locate_button);

        drawerMenu.setAmenity(amenity);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();

        slidingLayout = findView(R.id.sliding_panel);

        if (savedInstanceState == null) {
            slidingLayout.hidePanel();
        } else {
            if (savedInstanceState.containsKey(MERCHANT_ID_EXTRA)){
                long merchantId = savedInstanceState.getLong(MERCHANT_ID_EXTRA);
                selectedMerchant = getMerchant(merchantId);
                merchantDetails.setMerchant(selectedMerchant);
            }
        }

        slidingLayout.setAnchorPoint(0.5f);
        PanelSlideListener slideListener = new PanelSlideListener();
        slidingLayout.setPanelSlideListener(slideListener);

        if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)) {
            slideListener.onPanelCollapsed(null);
        }

        if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.ANCHORED)) {
            slideListener.onPanelAnchored(null);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDatabaseSyncing(MerchantsSyncService.isSyncing());

        if (!databaseSyncing) {
            startService(MerchantsSyncService.makeIntent(this, false));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_map, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                drawerToggle.setDrawerIndicatorEnabled(false);

                toolbar.setNavigationOnClickListener(v1 -> {
                    searchView.setIconified(true);
                    searchView.onActionViewCollapsed();
                });
            } else {
                searchView.setIconified(true);
                searchView.onActionViewCollapsed();
                drawerToggle.setDrawerIndicatorEnabled(true);
                toolbar.setNavigationOnClickListener(v1 -> drawer.openDrawer(GravityCompat.START));
            }
        });

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
                ListPopupWindow popup = new CurrenciesFilterPopup(this);
                popup.setAnchorView(findViewById(R.id.anchor_upper_right));
                popup.setHeight(drawer.getHeight() / 10 * 9);
                popup.show();

                popup.setOnDismissListener(() -> {
                    merchantsCacheFragment.getMerchantsCache().invalidate();
                    reloadMerchants();
                });

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // TODO
            return;
        }

        if (intent.hasExtra(MERCHANT_ID_EXTRA)) {
            slidingLayout.postDelayed(() -> {
                selectMerchant(intent.getLongExtra(MERCHANT_ID_EXTRA, -1), false);
                slidingLayout.anchorPanel();
            }, 1);

            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri merchantUri = intent.getData();
            long merchantId = Long.valueOf(merchantUri.getLastPathSegment());
            selectMerchant(merchantId, false);
            slidingLayout.anchorPanel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_AMENITY, amenity);

        if (selectedMerchant != null) {
            outState.putLong(MERCHANT_ID_EXTRA, selectedMerchant.getId());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == RESULT_OK) {
            moveToUserLocation();
        }

        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == RESULT_CANCELED) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE), 13));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.ANCHORED) || slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.EXPANDED)) {
            slidingLayout.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void networkAvailable() {
        startService(MerchantsSyncService.makeIntent(this, false));
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
            merchant.setId(data.getLong(data.getColumnIndex(Database.Merchants._ID)));
            merchant.setLatitude(data.getDouble(data.getColumnIndex(Database.Merchants.LATITUDE)));
            merchant.setLongitude(data.getDouble(data.getColumnIndex(Database.Merchants.LONGITUDE)));
            merchant.setAmenity(data.getString(data.getColumnIndex(Database.Merchants.AMENITY)));

            merchants.add(merchant);
        }

        onMerchantsLoaded(merchants);
    }

    private void onMerchantsLoaded(Collection<Merchant> merchants) {
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
    public void onAmenitySelected(Amenity amenity, String title) {
        drawer.closeDrawer(Gravity.LEFT);
        getSupportActionBar().setTitle(title);
        this.amenity = amenity;
        reloadMerchants();
    }

    @Override
    public void onSettingsSelected() {
        drawer.closeDrawer(Gravity.LEFT);
        startActivity(new Intent(this, SettingsActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
    }

    @Override
    public void onFeedbackSelected() {
        drawer.closeDrawer(Gravity.LEFT);
        startActivity(new Intent(this, FeedbackActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
    }

    private void showLoader() {
        if (loader.getVisibility() == View.VISIBLE) {
            return;
        }

        AlphaAnimation animation = new AlphaAnimation(0.7f, 1.0f);
        animation.setDuration(700);
        animation.setRepeatCount(AlphaAnimation.INFINITE);
        animation.setRepeatMode(AlphaAnimation.REVERSE);
        loader.setVisibility(View.VISIBLE);
        loader.startAnimation(animation);
    }

    private void hideLoader() {
        loader.setAnimation(null);
        loader.setVisibility(View.GONE);
    }

    private void moveToUserLocation() {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (lastLocation != null) {
            onUserLocationReceived(lastLocation);
        } else {
            requestLocationUpdate();
        }
    }

    private void requestLocationUpdate() {
        LocationRequest locationRequest = LocationRequest.create().setNumUpdates(1);
        LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
        PendingResult<LocationSettingsResult> locationSettingsResult = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest);

        locationSettingsResult.setResultCallback(result -> {
            switch (result.getStatus().getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this::onUserLocationReceived);
                    break;

                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        result.getStatus().startResolutionForResult(MapActivity.this, REQUEST_CHECK_LOCATION_SETTINGS);
                    } catch (IntentSender.SendIntentException exception) {
                        // Ignoring
                    }

                    break;
            }
        });
    }

    private void onUserLocationReceived(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));

        NotificationAreaProvider notificationAreaProvider = new NotificationAreaProvider(this);

        if (notificationAreaProvider.get() == null) {
            notificationAreaProvider.save(new NotificationArea(latLng));
        }
    }

    private void initClustering() {
        merchantsManager = new ClusterManager<>(this, map);
        PlacesRenderer renderer = new PlacesRenderer(this, map, merchantsManager);
        merchantsManager.setRenderer(renderer);
        renderer.setOnClusterItemClickListener(new ClusterItemClickListener());

        map.setOnCameraChangeListener(merchantsManager);
        map.setOnMarkerClickListener(merchantsManager);
        map.setOnMapClickListener(latLng -> {
            slidingLayout.hidePanel();
            selectedMerchant = null;
        });
    }

    @Subscribe
    public void onDatabaseSyncStarted(DatabaseSyncStartedEvent event) {
        setDatabaseSyncing(true);
    }

    @Subscribe
    public void onDatabaseSyncFinished(MerchantsSyncFinishedEvent event) {
        setDatabaseSyncing(false);

        if (event.isDataChanged()) {
            merchantsCacheFragment.getMerchantsCache().invalidate();
            reloadMerchants();
        }
    }

    @Subscribe
    public void onDatabaseSyncFailed(DatabaseSyncFailedEvent event) {
        setDatabaseSyncing(false);
    }

    private void reloadMerchants() {
        if (selectedMerchant != null && slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.ANCHORED)) {
            onMerchantsLoaded(Collections.singletonList(selectedMerchant));
            return;
        }

        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        MerchantsCache cache = merchantsCacheFragment.getMerchantsCache();

        if (cache.isInitialized()) {
            onMerchantsLoaded(cache.getMerchants(bounds, amenity));
        } else {
            getSupportLoaderManager().restartLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds, amenity), MapActivity.this);
        }
    }

    private Merchant getMerchant(long id) {
        Merchant merchant = new Merchant();

        Cursor cursor = getContentResolver().query(Database.Merchants.CONTENT_URI,
                new String[] {Database.Merchants._ID, Database.Merchants.LATITUDE, Database.Merchants.LONGITUDE, Database.Merchants.NAME, Database.Merchants.DESCRIPTION, Database.Merchants.PHONE, Database.Merchants.WEBSITE, Database.Merchants.ADDRESS, Database.Merchants.OPENING_HOURS, Database.Merchants.AMENITY },
                "_id = ?",
                new String[] { String.valueOf(id) },
                null);

        if (cursor.moveToNext()) {
            merchant.setId(cursor.getLong(cursor.getColumnIndex(Database.Merchants._ID)));
            merchant.setLatitude(cursor.getDouble(cursor.getColumnIndex(Database.Merchants.LATITUDE)));
            merchant.setLongitude(cursor.getDouble(cursor.getColumnIndex(Database.Merchants.LONGITUDE)));
            merchant.setName(cursor.getString(cursor.getColumnIndex(Database.Merchants.NAME)));
            merchant.setDescription(cursor.getString(cursor.getColumnIndex(Database.Merchants.DESCRIPTION)));
            merchant.setPhone(cursor.getString(cursor.getColumnIndex(Database.Merchants.PHONE)));
            merchant.setWebsite(cursor.getString(cursor.getColumnIndex(Database.Merchants.WEBSITE)));
            merchant.setAddress(cursor.getString(cursor.getColumnIndex(Database.Merchants.ADDRESS)));
            merchant.setOpeningHours(cursor.getString(cursor.getColumnIndex(Database.Merchants.OPENING_HOURS)));
            merchant.setAmenity(cursor.getString(cursor.getColumnIndex(Database.Merchants.AMENITY)));
        }

        cursor.close();

        cursor = getContentResolver().query(Database.Merchants.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).appendPath("currencies").build(),
                new String[] { Database.Currencies.NAME },
                null,
                null,
                null);

        merchant.setCurrencies(new ArrayList<>());

        while (cursor.moveToNext()) {
            Currency currency = new Currency();
            currency.setName(cursor.getString(0));
            merchant.getCurrencies().add(currency);
        }

        cursor.close();

        return merchant;
    }

    public void onActionButtonClicked(View view) {
        if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.ANCHORED)) {
            Utils.showDirections(this, selectedMerchant.getLatitude(), selectedMerchant.getLongitude());
        } else {
            moveToUserLocation();
        }
    }

    private void setDatabaseSyncing(boolean syncing) {
        if (databaseSyncing == syncing) {
            return;
        }

        databaseSyncing = syncing;

        if (databaseSyncing) {
            if (!slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.ANCHORED)) {
                showLoader();
            }
        } else {
            hideLoader();
        }
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

    private class PlacesRenderer extends StaticClusterRenderer<Merchant> {
        public PlacesRenderer(Context context, GoogleMap map, ClusterManager<Merchant> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(Merchant item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
            markerOptions.icon(markersCache.getMarker(item.getAmenity())).anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V);
        }
    }

    private class CameraChangeListener implements GoogleMap.OnCameraChangeListener {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            reloadMerchants();

            if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)) {
                saveCameraPositionFlag = true;
            }
        }
    }

    private class ClusterItemClickListener implements ClusterManager.OnClusterItemClickListener<Merchant> {
        @Override
        public boolean onClusterItemClick(Merchant merchant) {
            selectedMerchant = merchant;
            selectMerchant(merchant.getId(), true);
            return false;
        }
    }

    private void selectMerchant(long merchantId, boolean saveCameraPosition) {
        saveCameraPositionFlag = saveCameraPosition;
        selectedMerchant = getMerchant(merchantId);
        merchantDetails.setMerchant(selectedMerchant);

        slidingLayout.setPanelHeight(merchantDetails.getHeaderHeight());
        slidingLayout.showPanel();
    }

    private class LocationApiConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if (firstLaunch) {
                moveToUserLocation();
            }
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

    private class PanelSlideListener implements SlidingUpPanelLayout.PanelSlideListener {
        private boolean wasExpanded;

        @Override
        public void onPanelSlide(View view, float offset) {
            float locateButtonOffset = -offset * (view.getHeight() - merchantDetails.getHeaderHeight()) - merchantDetails.getHeaderHeight();
            actionButton.setTranslationY(Math.min(locateButtonOffset, 0));

            if (saveCameraPositionFlag) {
                cameraBeforeSelection = CameraUpdateFactory.newCameraPosition(map.getCameraPosition());
                saveCameraPositionFlag = false;
            }

            if (offset < 0.2) {
                if (!getSupportActionBar().isShowing()) {
                    getSupportActionBar().show();
                    merchantToolbar.setVisibility(View.GONE);
                    merchantTopGradient.setVisibility(View.GONE);
                }

                if (databaseSyncing) {
                    showLoader();
                }
            } else {
                if (getSupportActionBar().isShowing()) {
                    getSupportActionBar().hide();
                    merchantToolbar.setVisibility(View.VISIBLE);
                    merchantTopGradient.setVisibility(View.VISIBLE);
                }

                if (loader.getVisibility() == View.VISIBLE) {
                    hideLoader();
                }
            }
        }

        @Override
        public void onPanelCollapsed(View view) {
            slidingLayout.post(() -> {
                slidingLayout.setPanelHeight(merchantDetails.getHeaderHeight());
                actionButton.setTranslationY(-merchantDetails.getHeaderHeight());
            });

            if (!wasExpanded) {
                return;
            }

            map.setPadding(0, 0, 0, 0);
            map.getUiSettings().setAllGesturesEnabled(true);

            if (cameraBeforeSelection != null) {
                map.moveCamera(cameraBeforeSelection);
                cameraBeforeSelection = null;
                merchantDetails.postDelayed(() -> reloadMerchants(), 1);
            }

            actionButton.setImageResource(R.drawable.fab_location);
        }

        @Override
        public void onPanelExpanded(View view) {
            wasExpanded = true;
        }

        @Override
        public void onPanelAnchored(View view) {
            wasExpanded = true;
            map.setPadding(0, getResources().getDimensionPixelSize(R.dimen.marker_size) / 2, 0, Utils.getScreenHeight(MapActivity.this) / 2 + Utils.getStatusBarHeight(getApplicationContext()));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedMerchant.getPosition(), 13));
            map.getUiSettings().setAllGesturesEnabled(false);
            actionButton.setImageResource(R.drawable.fab_directions);
        }

        @Override
        public void onPanelHidden(View view) {
            selectedMerchant = null;
            wasExpanded = false;
            map.setPadding(0, 0, 0, 0);
            map.getUiSettings().setAllGesturesEnabled(true);
            actionButton.setTranslationY(0);
            actionButton.setImageResource(R.drawable.fab_location);
        }
    }
}