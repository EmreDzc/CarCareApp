package com.example.carcare.ProfilePage.Adapters; // Paket adını kendi yapınıza göre düzenleyin

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.carcare.ProfilePage.Fragments.BillingAddressFragment;
import com.example.carcare.ProfilePage.Fragments.DeliveryAddressFragment;

public class AddressPagerAdapter extends FragmentStateAdapter {

    public AddressPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DeliveryAddressFragment();
            case 1:
                return new BillingAddressFragment();
            default:
                return new DeliveryAddressFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}