package com.example.safehaven;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {
    private EditText name, email, password, confirmPassword, phoneNumber;
    private Spinner bloodType;
    private RadioGroup genderGroup;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize UI components
        name = findViewById(R.id.EtName);
        email = findViewById(R.id.EtEmail);
        password = findViewById(R.id.EtPassword);
        confirmPassword = findViewById(R.id.EtConfirmPswd_SignUp);
        phoneNumber = findViewById(R.id.EtPhoneNum);
        bloodType = findViewById(R.id.SpnBloodType);
        genderGroup = findViewById(R.id.RgGender);
        Button signUp = findViewById(R.id.BtnSignUp);

        auth = FirebaseAuth.getInstance();

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserData userData = collectUserData();
                String confirmPasswordInput = confirmPassword.getText().toString().trim();
                if(validateSignupInput(userData, confirmPasswordInput)) {
                    signUpUser(userData);
                }
            }
        });
    }

    // Collect user input into UserData object
    private UserData collectUserData() {
        String gender = genderGroup.getCheckedRadioButtonId() == R.id.RbMale
                ? "Male"
                : "Female";

        String bloodGroup = bloodType.getSelectedItem().toString();
        return new UserData(
                name.getText().toString().trim(),
                email.getText().toString().trim(),
                password.getText().toString().trim(),
                phoneNumber.getText().toString().trim(),
                bloodGroup,
                gender
        );
    }

    // Validate user input
    private boolean validateSignupInput(UserData userData, String confirmPasswordInput) {
        boolean isValid = true;
        if (!validateField(userData.email, email, "Please Enter your Email Address")) isValid = false;
        if (!validateField(userData.name, name, "Please Enter your Name")) isValid = false;
        if (!validateField(userData.password, password, "Please Enter your Password")) isValid = false;
        if (!validateField(confirmPasswordInput, confirmPassword, "Please Confirm your Password")) isValid = false;
        if (!validateField(userData.phoneNumber, phoneNumber, "Please Enter your Phone Number")) isValid = false;

        if (userData.bloodType.equals("Select Blood Group")) {
            Toast.makeText(this, "Please Select your Blood Group", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

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

        return isValid;
    }

    // Validate individual fields
    private boolean validateField(String value, EditText field, String errorMessage) {
        if (TextUtils.isEmpty(value)) {
            field.setError(errorMessage);
            return false;
        }
        return true;
    }

    // Sign up user using Firebase Authentication
    private void signUpUser(UserData userData) {
        auth.createUserWithEmailAndPassword(userData.email, userData.password)
                .addOnCompleteListener(task ->  {
                        if(task.isSuccessful()) {
                            // Debug
                            String userID = FirebaseAuth.getInstance().getCurrentUser() != null
                                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                                    : "null_user";

                            Log.d("Signup", "User registered successfully, UserID: " + userID);
                            Log.d("Signup", "Calling saveUserData with userID: " + userID);

                            saveUserData(userData, userID);
                            navigateToLogin();
                        } else {
                            Log.e("SignupError", task.getException().getMessage());
                            Toast.makeText(SignupActivity.this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignupActivity.this,
                                "Register failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Save User data into Firebase Realtime Database
    private void saveUserData(UserData userData, String userID) {
        Log.d("Database", "Saving data for userID: " + userID);

        FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users").child(userID)
                .setValue(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Database","User data saved successfully");
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

    // Initialize empty contacts for new user
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

    // Navigate to LoginActivity
    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToLogin();
    }


}