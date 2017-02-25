package com.bubelov.coins.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.ui.fragment.SignInFragment;
import com.bubelov.coins.ui.fragment.SignUpFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class EmailSignInActivity extends AbstractActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @BindView(R.id.pager)
    ViewPager tabPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_sign_in);
        ButterKnife.bind(this);

        tabPager.setAdapter(new TabsAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(tabPager);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supportFinishAfterTransition();
            }
        });
    }

    private class TabsAdapter extends FragmentPagerAdapter {
        private final List<Pair<Fragment, String>> pages;

        TabsAdapter(FragmentManager fm) {
            super(fm);
            pages = new ArrayList<>();
            pages.add(new Pair<Fragment, String>(new SignInFragment(), getString(R.string.sign_in)));
            pages.add(new Pair<Fragment, String>(new SignUpFragment(), getString(R.string.sign_up)));
        }

        @Override
        public Fragment getItem(int position) {
            return pages.get(position).first;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return pages.get(position).second;
        }

        @Override
        public int getCount() {
            return pages.size();
        }
    }
}