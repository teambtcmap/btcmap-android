package com.bubelov.coins.dagger;

import android.content.Context;

import com.bubelov.coins.repository.area.NotificationAreaRepository;
import com.bubelov.coins.repository.currency.CurrenciesRepository;
import com.bubelov.coins.repository.notification.PlaceNotificationsRepository;
import com.bubelov.coins.repository.place.PlacesRepository;
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceDb;
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceMemory;
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceNetwork;
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository;
import com.bubelov.coins.repository.rate.ExchangeRatesRepository;
import com.bubelov.coins.ui.activity.EditPlaceActivity;
import com.bubelov.coins.ui.activity.NotificationAreaActivity;
import com.bubelov.coins.ui.activity.ProfileActivity;
import com.bubelov.coins.ui.fragment.SettingsFragment;
import com.bubelov.coins.ui.fragment.SignInFragment;
import com.bubelov.coins.ui.fragment.SignUpFragment;
import com.bubelov.coins.database.sync.DatabaseSync;
import com.bubelov.coins.ui.viewmodel.MainViewModel;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = {MainModule.class})
public interface MainComponent {
    Context context();

    DatabaseSync databaseSync();

    FirebaseAnalytics analytics();

    PlaceNotificationManager notificationManager();

    NotificationAreaRepository notificationAreaRepository();
    PlacesRepository placesRepository();
    CurrenciesRepository currenciesRepository();
    ExchangeRatesRepository exchangeRatesRepository();
    PlaceNotificationsRepository placeNotificationsRepository();

    PlaceCategoriesRepository placeCategoriesRepository();
    PlaceCategoriesDataSourceNetwork placeCategoriesDataSourceNetwork();
    PlaceCategoriesDataSourceDb placeCategoriesDataSourceDb();
    PlaceCategoriesDataSourceMemory placeCategoriesDataSourceMemory();

    void inject(EditPlaceActivity target);
    void inject(NotificationAreaActivity target);
    void inject(ProfileActivity target);

    void inject(SignInFragment target);
    void inject(SignUpFragment target);
    void inject(SettingsFragment target);

    void inject(MainViewModel target);

    void inject(DatabaseSync sync);
}