package aviv.buzaglo.barca365;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton; // שינוי ל-MaterialButton
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // רכיבי ה-UI
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin; // שינינו מ-Button ל-MaterialButton
    private TextView tvSignUp, tvForgotPassword;
    private CheckBox cbRememberMe;

    // משתנה Firebase
    private FirebaseAuth mAuth;

    // משתני SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "BarcaPrefs";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASS = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();

        // אתחול SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // קישור לרכיבי התצוגה (כאן נעשה השינוי הגדול)
        initViews();

        // בדיקה האם יש נתונים שמורים
        checkRememberMePreferences();

        // הגדרת המאזינים
        setupListeners();
    }

    private void initViews() {
        // עדכון ה-IDs לפי קובץ ה-XML החדש שלך:

        // ב-XML החדש ה-ID הוא email_input
        etEmail = findViewById(R.id.email_input);

        // ב-XML החדש ה-ID הוא password_input
        etPassword = findViewById(R.id.password_input);

        // ב-XML החדש ה-ID הוא login_button
        btnLogin = findViewById(R.id.login_button);

        // ב-XML החדש ה-ID הוא sign_up_text
        tvSignUp = findViewById(R.id.sign_up_text);

        // ב-XML החדש ה-ID הוא forgot_password_text
        tvForgotPassword = findViewById(R.id.forgot_password_text);

        // ב-XML החדש ה-ID הוא remember_me_checkbox
        cbRememberMe = findViewById(R.id.remember_me_checkbox);
    }

    // --- שאר הקוד נשאר כמעט ללא שינוי, למעט תיקון קטן ב-updateUI ---

    private void checkRememberMePreferences() {
        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER, false);

        if (isRemembered) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPass = sharedPreferences.getString(KEY_PASS, "");

            etEmail.setText(savedEmail);
            etPassword.setText(savedPass);
            cbRememberMe.setChecked(true);
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });
    }

    private void performLogin() {
        // שימוש ב-Objects.requireNonNull כדי למנוע קריסה אם הטקסט ריק (נדיר ב-TextInputEditText אבל אפשרי)
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("נא להזין כתובת אימייל");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("נא להזין אימייל תקין");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("נא להזין סיסמה");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("CONNECTING..."); // שינוי לאנגלית שיתאים לעיצוב

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            manageRememberMe(email, password);
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            btnLogin.setEnabled(true);
                            btnLogin.setText("LOGIN"); // חזרה לטקסט המקורי
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error Logging In";
                            Toast.makeText(LoginActivity.this, "Login Failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void manageRememberMe(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (cbRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASS, password);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email above to reset password", Toast.LENGTH_LONG).show();
            etEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Reset email sent successfully", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            // הקוד הזה מונע חזרה למסך הלוגין בלחיצה על Back
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}