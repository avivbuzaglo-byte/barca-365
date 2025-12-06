package aviv.buzaglo.barca365;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton; // שימוש ב-MaterialButton
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    // רכיבי ה-UI
    // הערה: הסרנו את etPhone כי הוא לא קיים ב-XML החדש
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSignUp; // שינינו ל-MaterialButton
    private TextView tvLogin;

    // משתני Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 1. אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. קישור לרכיבים
        initViews();

        // 3. הגדרת מאזינים
        setupListeners();
    }

    private void initViews() {
        // עדכון ה-IDs לפי קובץ ה-XML החדש
        etFullName = findViewById(R.id.fullname_input);
        etEmail = findViewById(R.id.email_input);
        etPassword = findViewById(R.id.password_input);
        etConfirmPassword = findViewById(R.id.confirm_password_input);

        btnSignUp = findViewById(R.id.signup_button);
        tvLogin = findViewById(R.id.login_text);
    }

    private void setupListeners() {
        // לחיצה על כפתור הרשמה
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSignUp();
            }
        });

        // לחיצה על "Login" - חזרה למסך ההתחברות
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // סוגר את המסך וחוזר ל-Login
            }
        });
    }

    private void performSignUp() {
        // שליפת הטקסטים (בדיקה שהם לא null למניעת קריסה)
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // --- ולידציה (בדיקות תקינות) ---

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Please enter your full name");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // הסרנו את הבדיקה של הטלפון

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // --- יצירת המשתמש ב-Firebase ---

        btnSignUp.setEnabled(false);
        btnSignUp.setText("CREATING ACCOUNT..."); // טקסט באנגלית שיתאים לעיצוב

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // המשתמש נוצר בהצלחה
                            // שומרים את השם והאימייל (בלי טלפון)
                            saveUserToFirestore(fullName, email);
                        } else {
                            // כישלון
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("SIGN UP");
                            String error = task.getException() != null ? task.getException().getMessage() : "Registration Failed";
                            Toast.makeText(SignUpActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // הורדנו את הפרמטר phone מהפונקציה
    private void saveUserToFirestore(String name, String email) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // יצירת אובייקט נתונים לשמירה
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("fullName", name);
        userMap.put("email", email);
        // הסרנו את הטלפון מהשמירה

        // שמירה באוסף "Users"
        db.collection("Users").document(userId)
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(SignUpActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                        // מעבר למסך הראשי
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("SIGN UP");
                        Toast.makeText(SignUpActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}