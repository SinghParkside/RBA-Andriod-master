package edu.uwp.appfactory.wishope.views.landing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1></h1>
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 04-12-2020
 */
public class CommunicationTabAdapter extends FragmentStatePagerAdapter {

    private final List<String> tabTitles = new ArrayList<>();
    private final List<Fragment> tabFragments = new ArrayList<>();

    public CommunicationTabAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return tabFragments.get(position);
    }

    public void addFragment(Fragment fragment, String title) {
        tabFragments.add(fragment);
        tabTitles.add(title);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }


    @Override
    public int getCount() {
        return tabFragments.size();
    }
}
