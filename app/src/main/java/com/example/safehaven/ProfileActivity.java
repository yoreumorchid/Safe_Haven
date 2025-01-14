package com.example.safehaven;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private TextView email;
    private EditText name;
    private EditText phoneNumber;
    private RadioGroup genderGroup;
    private Button edit;
    private Button saveChanges;
    private Button logout;
    private Button updateEmail;
    private Button resetPassword;

    private FirebaseAuth auth;
    private String userID;
    private String currentPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI components
        name = findViewById(R.id.EtName);
        email = findViewById(R.id.TvEmail);
        phoneNumber = findViewById(R.id.EtPhoneNum);
        genderGroup = findViewById(R.id.RgGender);
        saveChanges = findViewById(R.id.BtnSaveChanges);
        resetPassword = findViewById(R.id.BtnResetPassword);
        updateEmail = findViewById(R.id.BtnUpdateEmail);
        logout = findViewById(R.id.BtnLogOut);
        edit = findViewById(R.id.BtnEdit);
        auth = FirebaseAuth.getInstance();
        userID = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        saveChanges.setVisibility(View.GONE);

        // Load user data
        DatabaseReference reference = FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");
        reference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData userProfile = snapshot.getValue(UserData.class);
                if (userProfile != null) {
                    name.setText(userProfile.name);
                    email.setText(userProfile.email);
                    phoneNumber.setText(userProfile.phoneNumber);
                    if (userProfile.gender.equalsIgnoreCase("Male")) {
                        genderGroup.check(R.id.RbMale);
                    } else if (userProfile.gender.equalsIgnoreCase("Female")) {
                        genderGroup.check(R.id.RbFemale);
                    }
                    currentPassword = userProfile.getPassword();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile data!", Toast.LENGTH_SHORT).show();
            }
        });

        // Edit button listener
        edit.setOnClickListener(v -> enableEditing());

        // Save changes button listener
        saveChanges.setOnClickListener(v -> saveUserChanges());

        resetPassword.setOnClickListener(v -> navigateToActivity(ForgotPasswordActivity.class));
        updateEmail.setOnClickListener(v -> navigateToActivity(UpdateEmailActivity.class));
        logout.setOnClickListener(v -> logoutUser());
    }

    private void saveUserChanges() {
        String updatedName = name.getText().toString().trim();
        String updatedPhoneNumber = phoneNumber.getText().toString().trim();
        int selectedId = genderGroup.getCheckedRadioButtonId();
        String selectedGender = selectedId == R.id.RbMale ? "Male" : "Female";

        if (updatedName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPhoneNumber(updatedPhoneNumber)) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference reference = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userID);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", updatedName);
        updates.put("phoneNumber", updatedPhoneNumber);
        updates.put("gender", selectedGender);

        reference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                disableEditing();
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update profile!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableEditing() {
        name.setEnabled(true);
        phoneNumber.setEnabled(true);
        for (int i = 0; i < genderGroup.getChildCount(); i++) {
            genderGroup.getChildAt(i).setEnabled(true);
        }
        saveChanges.setVisibility(View.VISIBLE);
        name.requestFocus();
    }

    private void disableEditing() {
        name.setEnabled(false);
        phoneNumber.setEnabled(false);
        for (int i = 0; i < genderGroup.getChildCount(); i++) {
            genderGroup.getChildAt(i).setEnabled(false);
        }
        saveChanges.setVisibility(View.GONE);
    }

    private void logoutUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        auth.signOut();
        navigateToActivity(LoginActivity.class);
        finish();
    }

    private void navigateToActivity(Class<?> targetActivity) {
        startActivity(new Intent(ProfileActivity.this, targetActivity));
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.replaceAll("[\\s\\-()]", "");
        return phoneNumber.matches("01\\d{8,9}");
    }
}
