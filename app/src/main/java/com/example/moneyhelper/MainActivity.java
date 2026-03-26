package com.example.moneyhelper;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Инициализируем планировщик прогнозов
//        PredictionScheduler.schedulePredictions(this);
        PDFBoxResourceLoader.init(getApplicationContext());

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Загружаем главный фрагмент по умолчанию
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }





        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = new HomeFragment();
            int itemId = item.getItemId();

            if (itemId == R.id.nav_categories) {
                selectedFragment = new CategoriesFragment();
            } else if (itemId == R.id.nav_expenses) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }
//            else if (itemId == R.id.nav_calendar){
//                selectedFragment = new PredictionCalendarFragment();
//            }

            loadFragment(selectedFragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}

