package aviv.buzaglo.barca365;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import aviv.buzaglo.barca365.fragments.HomeFragment;
import aviv.buzaglo.barca365.fragments.PlayersFragment;
import aviv.buzaglo.barca365.fragments.StandingsFragment;
import aviv.buzaglo.barca365.fragments.FixturesFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // משתנים לטיפול באינטרנט (הועתקו מ-SplashActivity)
    private BroadcastReceiver networkReceiver;
    private AlertDialog noInternetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bnvBar);

        // אתחול הרסיבר של האינטרנט
        initNetworkReceiver();

        // הגדרת ה-Listener לניווט תחתון
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

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // טעינה ראשונית
        loadFragment(new HomeFragment());
        bottomNavigationView.setSelectedItemId(R.id.home);

        // בדיקה ראשונית אם יש אינטרנט כשנכנסים לאפליקציה
        if (!isNetworkAvailable(this)) {
            showNoInternetDialog();
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // ------------------------------------------------------------------------
    //                לוגיקה של בדיקת אינטרנט (כמו ב-SplashActivity)
    // ------------------------------------------------------------------------

    private void initNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    handleNetworkChange();
                }
            }
        };
    }

    private void handleNetworkChange() {
        // אם האינטרנט חזר - נסגור את הדיאלוג
        if (isNetworkAvailable(this)) {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
            }
        } else {
            // אם האינטרנט נפל - נציג את הדיאלוג
            showNoInternetDialog();
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    //                          ניהול דיאלוג (אין אינטרנט)
    // ------------------------------------------------------------------------

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // משתמשים באותו עיצוב XML שיש לך ב-SplashActivity
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

        setupDialogButton(dialogView);

        builder.setView(dialogView);
        builder.setCancelable(false); // חובה שהמשתמש לא יוכל לסגור בלחיצה בצד
        noInternetDialog = builder.create();

        if (noInternetDialog.getWindow() != null) {
            noInternetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        noInternetDialog.show();
    }

    private void setupDialogButton(View dialogView) {
        Button btnRetry = dialogView.findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            // אנימציית לחיצה
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                performRetryCheck();
            }).start();
        });
    }

    private void performRetryCheck() {
        if (isNetworkAvailable(MainActivity.this)) {
            if (noInternetDialog != null) noInternetDialog.dismiss();
            Toast.makeText(MainActivity.this, "החיבור חזר!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "עדיין אין חיבור... בדוק את ה-WIFI", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------------
    //                          Lifecycle (רישום ומחיקת רסיבר)
    // ------------------------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }
}