package com.example.safehaven;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
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
    private String selectedGender;
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
                if(userProfile != null) {
                    name.setText(userProfile.name);
                    email.setText(userProfile.email);
                    phoneNumber.setText(userProfile.phoneNumber);
                    if(userProfile.gender.equalsIgnoreCase("Male")) {
                        genderGroup.check(R.id.RbMale);
                    } else if(userProfile.gender.equalsIgnoreCase("Female")) {
                        genderGroup.check(R.id.RbFemale);
                    }
                    currentPassword = userProfile.getPassword();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load profile data!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Edit button listener
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name.setEnabled(true);
                phoneNumber.setEnabled(true);
                for (int i = 0; i < genderGroup.getChildCount(); i++) {
                    genderGroup.getChildAt(i).setEnabled(true);
                }
                saveChanges.setVisibility(View.VISIBLE);
                name.requestFocus();
            }
        });

        // Save changes button listener
        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = genderGroup.getCheckedRadioButtonId();
                String selectedGender = selectedId == R.id.RbMale ? "Male" : "Female";

                // Update user information
                String updated_name = name.getText().toString().trim();
                String updated_phoneNumber = phoneNumber.getText().toString().trim();
                UserData updatedUserProfile = new UserData(updated_name,
                        email.getText().toString().trim(),
                        currentPassword,
                        updated_phoneNumber,
                        selectedGender
                );
                reference.child(userID).setValue(updatedUserProfile).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        disableEditing();
                        Toast.makeText(ProfileActivity.this,
                                "Profile updated successfully!",
                                Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    disableEditing();
                    Toast.makeText(ProfileActivity.this,
                            "Something went wrong. Try again!",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(ProfileActivity.this, ForgotPasswordActivity.class));
                    finish();
                } catch(Exception e) {
                    Toast.makeText(ProfileActivity.this,
                            "Something went wrong. Try again!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, UpdateEmailActivity.class));
                finish();
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                Toast.makeText(ProfileActivity.this,
                        "Logout Successful",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void disableEditing() {
        name.setEnabled(false);
        phoneNumber.setEnabled(false);
        for (int i = 0; i < genderGroup.getChildCount(); i++) {
            genderGroup.getChildAt(i).setEnabled(false);
        }
        saveChanges.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (saveChanges.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please save your changes before exiting!", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        }
    }

}