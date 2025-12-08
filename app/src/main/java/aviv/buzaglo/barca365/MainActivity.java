package aviv.buzaglo.barca365;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
                // selectedFragment = new HomeFragment(); // נפתח בהמשך
            } else if (itemId == R.id.players) {
                // כאן אנחנו יוצרים את פרגמנט השחקנים שבנינו
                selectedFragment = new PlayersFragment();
            } else if (itemId == R.id.matches) {
                selectedFragment = new FixturesFragment();
            }
            else if (itemId == R.id.table){
                selectedFragment = new StandingsFragment();
            }

            // אם נבחר פרגמנט, טען אותו
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // טעינת ברירת מחדל: כרגע נטען את Players כי הוא היחיד שמוכן
        // כשתבנה את Home, תחליף את השורה הזו
        loadFragment(new PlayersFragment());
        bottomNavigationView.setSelectedItemId(R.id.players); // מסמן את הכפתור הנכון ויזואלית
    }

    // פונקציית עזר להחלפת פרגמנטים
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment) // חייב להיות אותו ID כמו ב-XML
                .commit();
    }
}