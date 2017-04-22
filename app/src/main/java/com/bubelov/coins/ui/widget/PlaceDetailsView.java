package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.util.Analytics;
import com.bubelov.coins.util.Utils;

import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class PlaceDetailsView extends FrameLayout {
    @BindView(R.id.header_switcher)
    ViewSwitcher headerSwitcher;

    @BindView(R.id.name)
    TextView name;

    @BindView(R.id.check_mark)
    View checkMark;

    @BindView(R.id.warning)
    View warning;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.opened_claims)
    TextView openedClaims;

    @BindView(R.id.closed_claims)
    TextView closedClaims;

    @BindView(R.id.phone)
    TextView phone;

    @BindView(R.id.website)
    TextView website;

    @BindView(R.id.description)
    TextView description;

    @BindView(R.id.opening_hours)
    TextView openingHours;

    @BindView(R.id.accepted_currencies)
    TextView acceptedCurrencies;

    Place place;

    Listener listener;

    public PlaceDetailsView(Context context) {
        super(context);
        init();
    }

    public PlaceDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlaceDetailsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int getHeaderHeight() {
        return headerSwitcher.getHeight();
    }

    public void setFullScreen(boolean fullScreen) {
        if (fullScreen) {
            headerSwitcher.setDisplayedChild(1);
        } else {
            headerSwitcher.setDisplayedChild(0);
        }
    }

    public void setPlace(final Place place) {
        this.place = place;

        if (place.getOpenedClaims() > 0 && place.getClosedClaims() == 0) {
            checkMark.setVisibility(VISIBLE);
        } else {
            checkMark.setVisibility(GONE);
        }

        if (place.getClosedClaims() > 0) {
            warning.setVisibility(VISIBLE);
        } else {
            warning.setVisibility(GONE);
        }

        if (place.getOpenedClaims() > 0) {
            openedClaims.setVisibility(VISIBLE);
            openedClaims.setText(getResources().getQuantityString(R.plurals.confirmed_by_d_users, place.getOpenedClaims(), place.getOpenedClaims()));
        } else {
            openedClaims.setVisibility(GONE);
        }

        if (place.getClosedClaims() > 0) {
            closedClaims.setVisibility(VISIBLE);
            closedClaims.setText(getResources().getQuantityString(R.plurals.marked_as_closed_by_d_users, place.getClosedClaims(), place.getClosedClaims()));
        } else {
            closedClaims.setVisibility(GONE);
        }

        if (TextUtils.isEmpty(place.getName())) {
            name.setText(R.string.name_unknown);
            toolbar.setTitle(R.string.name_unknown);
        } else {
            name.setText(place.getName());
            toolbar.setTitle(place.getName());
        }

        if (TextUtils.isEmpty(place.getPhone())) {
            phone.setText(R.string.not_provided);
        } else {
            phone.setText(place.getPhone());
        }

        if (TextUtils.isEmpty(place.getWebsite())) {
            website.setText(R.string.not_provided);
            website.setTextColor(getResources().getColor(R.color.black));
            website.setPaintFlags(website.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));

            website.setOnClickListener(null);
        } else {
            website.setText(place.getWebsite());
            website.setTextColor(getResources().getColor(R.color.primary_dark));
            website.setPaintFlags(website.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            website.setOnClickListener(view -> Utils.openUrl(PlaceDetailsView.this.getContext(), place.getWebsite()));
        }

        if (TextUtils.isEmpty(place.getDescription())) {
            description.setText(R.string.not_provided);
        } else {
            description.setText(place.getDescription());
        }

        if (TextUtils.isEmpty(place.getOpeningHours())) {
            openingHours.setText(R.string.not_provided);
        } else {
            openingHours.setText(place.getOpeningHours());
        }

        StringBuilder currenciesString = new StringBuilder();
        Iterator<Currency> currenciesIterator = place.getCurrencies().iterator();

        while (currenciesIterator.hasNext()) {
            currenciesString.append(currenciesIterator.next().getName());

            if (currenciesIterator.hasNext()) {
                currenciesString.append(", ");
            }
        }

        acceptedCurrencies.setText(currenciesString.length() == 0
                ? getContext().getString(R.string.not_provided)
                : currenciesString.toString());
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void init() {
        inflate(getContext(), R.layout.widget_place_details, this);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(view -> {
            if (listener != null) {
                listener.onDismissed();
            }
        });

        toolbar.inflateMenu(R.menu.place_details);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                Utils.share(getContext(), getResources().getString(R.string.share_place_message_title), getResources().getString(R.string.share_place_message_text, String.format("https://www.google.com/maps/@%s,%s,19z?hl=en", place.getLatitude(), place.getLongitude())));
                Analytics.logShareContentEvent(String.valueOf(place.getId()), place.getName(), "place");
                return true;
            }

            return false;
        });
    }

    public interface Listener {
        void onEditPlaceClick(Place place);

        void onDismissed();
    }

    @OnClick(R.id.edit)
    public void onEditClick() {
        if (listener != null) {
            listener.onEditPlaceClick(place);
        }
    }
}