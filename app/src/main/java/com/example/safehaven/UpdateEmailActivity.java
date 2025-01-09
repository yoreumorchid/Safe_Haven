package com.example.safehaven;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class UpdateEmailActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference reference;
    private String userID;
    private TextView name;
    private EditText etOldEmail, etNewEmail, etPassword;
    private Button btnUpdateEmail;

    private static final String TAG = "UpdateEmailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_email);

        // Initialize Firebase and UI components
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        userID = currentUser != null ? currentUser.getUid() : null;

        name = findViewById(R.id.TvName);
        etOldEmail = findViewById(R.id.EtOldEmail);
        etNewEmail = findViewById(R.id.EtNewEmail);
        etPassword = findViewById(R.id.EtPassword);
        btnUpdateEmail = findViewById(R.id.BtnUpdateEmailConfirm);

        reference = FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        // Load user profile
        loadUserProfile();

        // Set click listener for updating email
        btnUpdateEmail.setOnClickListener(v -> updateEmail());
    }

    private void loadUserProfile() {
        if (userID != null) {
            reference.child(userID).get().addOnSuccessListener(snapshot -> {
                UserData userProfile = snapshot.getValue(UserData.class);
                if (userProfile != null) {
                    name.setText(userProfile.name);
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(UpdateEmailActivity.this, "Failed to load profile data!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading profile: " + e.getMessage());
            });
        } else {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmail() {
        String oldEmail = etOldEmail.getText().toString().trim();
        String newEmail = etNewEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldEmail) || TextUtils.isEmpty(newEmail) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(oldEmail, password);
        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Re-authentication successful.");

                // Update email in Firebase Authentication
                currentUser.updateEmail(newEmail).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Log.d(TAG, "Email updated successfully in Firebase Authentication.");

                        // Send email verification for the new email
                        currentUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                            if (verifyTask.isSuccessful()) {
                                Log.d(TAG, "Verification email sent to: " + newEmail);
                                Toast.makeText(this, "Verification email sent. Please verify your new email.", Toast.LENGTH_SHORT).show();

                                // Update email in Realtime Database
                                updateEmailInDatabase(newEmail);
                            } else {
                                Log.e(TAG, "Failed to send verification email: " + verifyTask.getException().getMessage());
                                Toast.makeText(this, "Failed to send verification email. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to update email: " + Objects.requireNonNull(updateTask.getException()).getMessage());
                        Toast.makeText(this, "Failed to update email: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Log.e(TAG, "Re-authentication failed: " + Objects.requireNonNull(task.getException()).getMessage());
                Toast.makeText(this, "Re-authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateEmailInDatabase(String newEmail) {
        if (userID != null) {
            reference.child(userID).child("email").setValue(newEmail)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Email updated successfully in Realtime Database.");
                        Toast.makeText(this, "Email updated successfully in database!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update email in database: " + e.getMessage());
                        Toast.makeText(this, "Failed to update email in database!", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User ID is null. Cannot update email in database.", Toast.LENGTH_SHORT).show();
        }
    }
}