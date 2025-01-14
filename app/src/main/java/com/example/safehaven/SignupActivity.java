package com.example.safehaven;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public class SignupActivity extends AppCompatActivity {
    private EditText name, email, password, confirmPassword, phoneNumber;
    private RadioGroup genderGroup;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        name = findViewById(R.id.EtName);
        email = findViewById(R.id.EtEmail);
        password = findViewById(R.id.EtPassword);
        confirmPassword = findViewById(R.id.EtConfirmPswd_SignUp);
        phoneNumber = findViewById(R.id.EtPhoneNum);
        genderGroup = findViewById(R.id.RgGender);
        Button signUp = findViewById(R.id.BtnSignUp);

        auth = FirebaseAuth.getInstance();

        signUp.setOnClickListener(v -> {
            UserData userData = collectUserData();
            String confirmPasswordInput = confirmPassword.getText().toString().trim();
            if (validateSignupInput(userData, confirmPasswordInput)) {
                signUpUser(userData);
                navigateToLogin();
            }
        });
    }

    private UserData collectUserData() {
        String gender = genderGroup.getCheckedRadioButtonId() == R.id.RbMale
                ? "Male"
                : "Female";

        return new UserData(
                name.getText().toString().trim(),
                email.getText().toString().trim(),
                password.getText().toString().trim(),
                phoneNumber.getText().toString().trim(),
                gender
        );
    }

    private boolean validateSignupInput(UserData userData, String confirmPasswordInput) {
        boolean isValid = validateField(userData.email, email, "Please Enter your Email Address");
        if (!validateField(userData.name, name, "Please Enter your Name")) isValid = false;
        if (!validateField(userData.password, password, "Please Enter your Password")) isValid = false;
        if (!validateField(confirmPasswordInput, confirmPassword, "Please Confirm your Password")) isValid = false;
        if (!validateField(userData.phoneNumber, phoneNumber, "Please Enter your Phone Number")) isValid = false;

        if (!userData.password.equals(confirmPasswordInput)) {
            confirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (userData.password.length() < 8) {
            password.setError("Password too short\nMinimum length 8 characters!");
            confirmPassword.setError("Password too short\nMinimum length 8 characters!");
            isValid = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userData.email).matches()) {
            email.setError("Invalid Email Address");
            isValid = false;
        }

        if (!isValidPhoneNumber(userData.phoneNumber)) {
            phoneNumber.setError("Invalid phone number.");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateField(String value, EditText field, String errorMessage) {
        if (TextUtils.isEmpty(value)) {
            field.setError(errorMessage);
            return false;
        }
        return true;
    }

    // Validate phone number
    private boolean isValidPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.replaceAll("[\\s\\-()]", "");
        return phoneNumber.matches("01\\d{8,9}");
    }

    private void signUpUser(UserData userData) {
        String hashedPassword = hashPassword(userData.password);
        if (hashedPassword == null) {
            Toast.makeText(this, "Password hashing failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        UserData userDataWithHashedPassword = new UserData(
                userData.name,
                userData.email,
                hashedPassword,
                userData.phoneNumber,
                userData.gender
        );

        auth.createUserWithEmailAndPassword(userData.email, userData.password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userID = FirebaseAuth.getInstance().getCurrentUser() != null
                                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                                : "null_user";

                        Log.d("Signup", "User registered successfully, UserID: " + userID);
                        saveUserData(userDataWithHashedPassword, userID);
                    } else {
                        Log.e("SignupError", Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        Toast.makeText(SignupActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SignupActivity.this,
                        "Register failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private String hashPassword(String plainPassword) {
        try {
            return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        } catch (Exception e) {
            Log.e("HashError", "Error hashing password: " + e.getMessage());
            return null;
        }
    }

    private void saveUserData(UserData userData, String userID) {
        Log.d("Database", "Saving data for userID: " + userID);

        FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users").child(userID)
                .setValue(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Database", "User data saved successfully");
                            initializeUserContacts(userID);
                        } else {
                            Log.e("Database", "Failed to save user data");
                            Toast.makeText(SignupActivity.this,
                                    "Failed to save user data.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignupActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initializeUserContacts(String userID) {
        Contacts contacts = new Contacts("", "", "", "");
        FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users").child(userID).child("Contacts")
                .setValue(contacts)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        navigateToLogin();
                    } else {
                        Toast.makeText(this,
                                "Failed to initialize contacts",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        finish();
    }
}