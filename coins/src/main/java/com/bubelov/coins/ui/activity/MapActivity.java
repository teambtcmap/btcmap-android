package com.bubelov.coins.ui.activity;

import android.animation.Animator;
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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Tables;
import com.bubelov.coins.event.MerchantsSyncFinishedEvent;
import com.bubelov.coins.event.NewMerchantsLoadedEvent;
import com.bubelov.coins.loader.MerchantsLoader;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.ui.fragment.CurrenciesFilterDialogFragment;
import com.bubelov.coins.ui.widget.DrawerMenu;
import com.bubelov.coins.ui.widget.MerchantDetailsView;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.bubelov.coins.util.StaticClusterRenderer;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collection;

public class MapActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<Cursor>, DrawerMenu.OnMenuItemSelectedListener, CurrenciesFilterDialogFragment.Listener {
    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int MERCHANTS_LOADER = 0;

    private DrawerLayout drawer;

    private ActionBarDrawerToggle drawerToggle;

    private GoogleMap map;

    private ClusterManager<Merchant> merchantsManager;

    private String amenity;

    private SlidingUpPanelLayout slidingLayout;

    private MerchantDetailsView merchantDetails;

    private View loader;

    private BitmapDescriptor merchantDescriptor;

    public static Intent newShowMerchantIntent(Context context, double latitude, double longitude) {
        return new Intent(context, MapActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new DrawerToggle(this, drawer, android.R.string.ok, android.R.string.ok);
        drawer.setDrawerListener(drawerToggle);

        DrawerMenu drawerMenu = (DrawerMenu) findViewById(R.id.left_drawer);
        drawerMenu.setSelected(R.id.all);
        drawerMenu.setItemSelectedListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMyLocationEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);

        initClustering();

        map.setOnCameraChangeListener(new OnCameraChangeMultiplexer(merchantsManager, new CameraChangeListener()));

        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        getSupportLoaderManager().initLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds, amenity), this);

        merchantDetails = findView(R.id.merchant_details);

        loader = findView(R.id.loader);
        loader.animate().alpha(0.7f).setDuration(700).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (loader.getAlpha() == 0.7f) {
                    loader.animate().alpha(1.0f).setDuration(700).setListener(this);
                } else {
                    loader.animate().alpha(0.7f).setDuration(700).setListener(this);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        merchantDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_place_white_48dp);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();

        slidingLayout = findView(R.id.sliding_panel);
        slidingLayout.setPanelHeight(0);
        slidingLayout.setAnchorPoint(0.5f);
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
                new CurrenciesFilterDialogFragment().show(getSupportFragmentManager(), CurrenciesFilterDialogFragment.TAG);
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
            merchant.setId(data.getLong(data.getColumnIndex(Tables.Merchants._ID)));
            merchant.setLatitude(data.getDouble(data.getColumnIndex(Tables.Merchants.LATITUDE)));
            merchant.setLongitude(data.getDouble(data.getColumnIndex(Tables.Merchants.LONGITUDE)));
            merchant.setAmenity(data.getString(data.getColumnIndex(Tables.Merchants.AMENITY)));

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
    public void onMenuItemSelected(int id, com.bubelov.coins.ui.widget.MenuItem menuItem) {
        drawer.closeDrawer(Gravity.LEFT);

        if (id != R.id.settings && id != R.id.help && id != R.id.donate) {
            getSupportActionBar().setTitle(menuItem.getText());
        }

        if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
            return;
        }

        switch (id) {
            case R.id.all:
                amenity = null;
                break;
            case R.id.atms:
                amenity = "atm";
                break;
            case R.id.cafes:
                amenity = "cafe";
                break;
            case R.id.restaurants:
                amenity = "restaurant";
                break;
            case R.id.bars:
                amenity = "bar";
                break;
            case R.id.hotels:
                amenity = "TODO";
                break;
            case R.id.car_washes:
                amenity = "car_wash";
                break;
            case R.id.gas_stations:
                amenity = "fuel";
                break;
            case R.id.hospitals:
                amenity = "hospital";
                break;
            case R.id.laundry:
                amenity = "TODO";
                break;
            case R.id.movies:
                amenity = "cinema";
                break;
            case R.id.parking:
                amenity = "parking";
                break;
            case R.id.pharmacies:
                amenity = "pharmacy";
                break;
            case R.id.pizza:
                amenity = "TODO";
                break;
            case R.id.taxi:
                amenity = "taxi";
                break;
        }

        reloadMerchants();
    }

    @Override
    public void onCurrenciesFilterDismissed() {
        reloadMerchants();
    }

    private void initClustering() {
        merchantsManager = new ClusterManager<>(this, map);
        PlacesRenderer renderer = new PlacesRenderer(this, map, merchantsManager);
        merchantsManager.setRenderer(renderer);
        renderer.setOnClusterItemClickListener(new ClusterItemClickListener());

        map.setOnCameraChangeListener(merchantsManager);
        map.setOnMarkerClickListener(merchantsManager);
        map.setOnMapClickListener(latLng -> slidingLayout.setPanelHeight(0));
    }

    @Subscribe
    public void onNewMerchantsLoaded(NewMerchantsLoadedEvent event) {
        loader.setVisibility(View.VISIBLE);
        reloadMerchants();
    }

    @Subscribe
    public void onMerchantSyncFinished(MerchantsSyncFinishedEvent event) {
        loader.setVisibility(View.GONE);
    }

    private void reloadMerchants() {
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        MerchantsCache cache = getApp().getMerchantsCache();

        if (cache.isInitialized()) {
            onMerchantsLoaded(cache.getMerchants(bounds, amenity));
        } else {
            getSupportLoaderManager().restartLoader(MERCHANTS_LOADER, MerchantsLoader.prepareArguments(bounds, amenity), MapActivity.this);
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
            markerOptions.icon(merchantDescriptor);
        }
    }

    private class CameraChangeListener implements GoogleMap.OnCameraChangeListener {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            reloadMerchants();
        }
    }

    private class ClusterItemClickListener implements ClusterManager.OnClusterItemClickListener<Merchant> {
        @Override
        public boolean onClusterItemClick(Merchant merchant) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();
//
//                    Random random = new Random(666);
//
//                    long start = System.currentTimeMillis();
//
//                    for (int i = 0; i < 500; i++) {
//                        Cursor cursor = db.rawQuery("select distinct m._id, m.latitude, m.longitude, m.name, m.description from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where (latitude between ? and ?) and (longitude between ? and ?) and c.show_on_map = 1",
//                                new String[] { String.valueOf(-180.0f + random.nextFloat() * 360.0f), String.valueOf(-180.0f + random.nextFloat() * 360.0f), String.valueOf(-180.0f + random.nextFloat() * 360.0f), String.valueOf(-180.0f + random.nextFloat() * 360.0f) });
//
//                        //Log.d(TAG, "Count: " + cursor.getCount());
//
//                        while (cursor.moveToNext()) {
//                            int x = 1;
//                        }
//
//                        cursor.close();
//                    }
//
//                    long execTime = System.currentTimeMillis() - start;
//                    Log.d(TAG, "Execution time: " + execTime);
//                    Log.d(TAG, "Avg query: " + (float) execTime / 10000.0f);
//                }
//            }).start();

            merchantDetails.setMerchant(merchant);
            slidingLayout.setPanelHeight(merchantDetails.getHeaderHeight());
            return false;
        }
    }
}