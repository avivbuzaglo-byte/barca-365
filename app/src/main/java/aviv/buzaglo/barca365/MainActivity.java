package aviv.buzaglo.barca365;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import aviv.buzaglo.barca365.fragments.HomeFragment;
import aviv.buzaglo.barca365.fragments.PlayersFragment;
import aviv.buzaglo.barca365.fragments.StandingsFragment;
import aviv.buzaglo.barca365.fragments.FixturesFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bnvBar);

        // הגדרת ה-Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.players) {
                selectedFragment = new PlayersFragment();
            } else if (itemId == R.id.fixtures) {
                selectedFragment = new FixturesFragment();
            }
            else if (itemId == R.id.standings){
                selectedFragment = new StandingsFragment();
            }

            // אם נבחר פרגמנט, טען אותו
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        loadFragment(new HomeFragment());
        bottomNavigationView.setSelectedItemId(R.id.home);
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment) // חייב להיות אותו ID כמו ב-XML
                .commit();
    }
}