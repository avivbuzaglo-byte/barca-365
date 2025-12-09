package aviv.buzaglo.barca365;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // שינינו ל-View כדי שיתאים גם ל-CardView
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserSettingsActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI Elements
    // שינינו ל-View כי ב-XML זה MaterialCardView ולא כפתור רגיל
    private View btnResetPassword;
    private View btnSignOut;
    private View btnDeleteAccount;

    // אלמנטים של הפרופיל
    private TextView tvProfileName, tvProfileEmail;
    private TextView tvProfileInitials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        // 1. אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserProfile(); // טעינת השם והאימייל לראש המסך
        setClickListeners();
    }

    private void initViews() {
        // וודא שה-IDs האלו תואמים ל-XML שלך!
        // (ב-XML קראנו לזה btn_sign_out וכו', אז צריך להתאים)
        btnResetPassword = findViewById(R.id.btn_reset_password);
        btnSignOut = findViewById(R.id.btn_sign_out);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);

        // טקסטים של הפרופיל (תוסיף להם ID ב-XML אם אין עדיין)
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileInitials = findViewById(R.id.tv_profile_initials);

        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    // --- טעינת פרטי משתמש ל-Header ---
    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvProfileEmail.setText(user.getEmail());

            db.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // שליפת השם המלא
                            String fullName = documentSnapshot.getString("fullName");

                            if (fullName != null && !fullName.isEmpty()) {
                                tvProfileName.setText(fullName);

                                // 3. חישוב והצגת ראשי התיבות
                                String initials = getInitials(fullName);
                                tvProfileInitials.setText(initials);
                            }
                        }
                    });
        }
    }
    private String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";

        String[] parts = fullName.trim().split("\\s+"); // פיצול לפי רווחים
        String initials = "";

        if (parts.length >= 2) {
            // אם יש שם פרטי ושם משפחה: "Lionel Messi" -> "LM"
            String first = parts[0];
            String last = parts[parts.length - 1]; // לוקחים את האחרון למקרה שיש שם אמצעי

            if (!first.isEmpty()) initials += first.charAt(0);
            if (!last.isEmpty()) initials += last.charAt(0);

        } else if (parts.length == 1) {
            // אם יש רק שם אחד: "Pedri" -> "PE" (שתי האותיות הראשונות)
            String name = parts[0];
            if (name.length() >= 2) {
                initials = name.substring(0, 2);
            } else {
                initials = name;
            }
        }

        return initials.toUpperCase(); // מחזיר באותיות גדולות
    }

    private void setClickListeners() {
        btnResetPassword.setOnClickListener(v -> handleResetPassword());
        btnSignOut.setOnClickListener(v -> showSignOutDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    // --- לוגיקה 1: איפוס סיסמה ---
    private void handleResetPassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(aVoid -> {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Check your Email")
                                .setMessage("We sent a password reset link to " + user.getEmail())
                                .setPositiveButton("OK", null)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // --- לוגיקה 2: התנתקות ---
    private void showSignOutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> performSignOut())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performSignOut() {
        mAuth.signOut(); // התנתקות מ-Firebase

        // מעבר למסך הלוגין
        Intent intent = new Intent(UserSettingsActivity.this, LoginActivity.class);
        // ניקוי ההיסטוריה כדי שלא יוכל לחזור אחורה בכפתור Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- לוגיקה 3: מחיקת חשבון (מסוכן!) ---
    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This will delete all your data permanently.")
                .setPositiveButton("Delete", (dialog, which) -> performDeleteAccount()) // דילגתי על שלב ה-Confirmation השני לפשטות, אבל אפשר להשאיר אותו
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDeleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // 1. מחיקת המידע מ-Firestore
        db.collection("Users").document(uid).delete()
                .addOnSuccessListener(aVoid -> {

                    // 2. מחיקת המשתמש מ-Auth
                    user.delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Account Deleted", Toast.LENGTH_SHORT).show();

                                // חזרה למסך כניסה
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                // הערה: אם עבר הרבה זמן מאז הלוגין, המחיקה תיכשל.
                                // במקרה כזה צריך לבקש מהמשתמש להתחבר מחדש (Re-authenticate).
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Data Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}