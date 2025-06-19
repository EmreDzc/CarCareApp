package com.example.carcare.ProfilePage.address;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2; // Eklendi

import com.example.carcare.ProfilePage.Adapters.AddressPagerAdapter; // Eklendi
import com.example.carcare.R;
import com.google.android.material.tabs.TabLayout; // Eklendi
import com.google.android.material.tabs.TabLayoutMediator; // Eklendi

public class AddressActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AddressPagerAdapter addressPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        ImageButton btnBack = findViewById(R.id.btn_back);
        tabLayout = findViewById(R.id.tab_layout_address);
        viewPager = findViewById(R.id.view_pager_address);

        btnBack.setOnClickListener(v -> finish());

        // ViewPager için adapter'ı ayarla
        addressPagerAdapter = new AddressPagerAdapter(this);
        viewPager.setAdapter(addressPagerAdapter);

        // TabLayout'u ViewPager2 ile senkronize et
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("My Delivery Addresses");
                    break;
                case 1:
                    tab.setText("My Billing Addresses");
                    break;
            }
        }).attach();

    }
}
