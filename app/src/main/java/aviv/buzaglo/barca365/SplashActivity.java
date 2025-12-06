package aviv.buzaglo.barca365;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 שניות

    // קבועים עבור SharedPreferences
    private static final String PREFS_NAME = "BarcaPrefs";
    private static final String KEY_REMEMBER = "remember_me";

    // משתנים
    private boolean isAnimationDone = false;
    private BroadcastReceiver networkReceiver;
    private AlertDialog noInternetDialog;

    // משתנה Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // הגדרת העיצוב (הלוגו שבחרנו)
        setContentView(R.layout.activity_splash);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();

        // הסתרת ה-ActionBar אם הוא קיים (למרות שהגדרנו ב-Manifest שלא יהיה)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initNetworkReceiver();
        startSplashTimer();
    }

    private void startSplashTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isAnimationDone = true;
            checkConnectionAndProceed();
        }, SPLASH_DURATION);
    }

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
        if (isNetworkAvailable(this)) {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
            }
            if (isAnimationDone) {
                decideNextActivity();
            }
        }
    }

    private void checkConnectionAndProceed() {
        if (isNetworkAvailable(this)) {
            decideNextActivity();
        } else {
            showNoInternetDialog();
        }
    }

    /**
     * הפונקציה החשובה: מחליטה לאן לנווט לפי מצב ההתחברות
     */
    private void decideNextActivity() {
        if (isFinishing()) return;

        // 1. בדיקה האם המשתמש מחובר טכנית ב-Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 2. בדיקה האם המשתמש סימן "זכור אותי"
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isRemembered = prefs.getBoolean(KEY_REMEMBER, false);

        Intent intent;

        // התנאי: גם מחובר לפיירבייס וגם סימן V בתיבה
        if (currentUser != null && isRemembered) {
            // הולכים ישר לבית
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // הולכים למסך התחברות
            // שים לב: וודא שיש לך קובץ LoginActivity בפרויקט, אחרת זה יסומן באדום
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        // ניקוי היסטוריה ומעבר
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ------------------------------------------------------------------------
    //                          ניהול דיאלוג (אין אינטרנט)
    // ------------------------------------------------------------------------

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // וודא שיש לך את הקובץ dialog_no_internet.xml ב-Layouts
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

        setupDialogButton(dialogView);

        builder.setView(dialogView);
        builder.setCancelable(false);
        noInternetDialog = builder.create();

        if (noInternetDialog.getWindow() != null) {
            noInternetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        noInternetDialog.show();
    }

    private void setupDialogButton(View dialogView) {
        Button btnRetry = dialogView.findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            // אנימציית לחיצה קטנה
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                performRetryCheck();
            }).start();
        });
    }

    private void performRetryCheck() {
        if (isNetworkAvailable(SplashActivity.this)) {
            if (noInternetDialog != null) noInternetDialog.dismiss();
            decideNextActivity();
        } else {
            Toast.makeText(SplashActivity.this, "עדיין אין חיבור... בדוק את ה-WIFI", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------------
    //                          פונקציות עזר + Lifecycle
    // ------------------------------------------------------------------------

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

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