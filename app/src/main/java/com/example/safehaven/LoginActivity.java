package com.example.safehaven;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private Button login;
    private TextView signup, forgotPassword;
    private EditText email, password;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        // if logged in,go to MainActivity
        if (isLoggedIn && auth.getCurrentUser() != null) {
            Log.d("LoginActivity", "User is still logged in, navigating to MainActivity.");
            navigateTo(MainActivity.class);
            finish();
            return;
        }

        email = findViewById(R.id.EtUsername);
        password = findViewById(R.id.EtPassword);
        login = findViewById(R.id.BtnLogin);
        forgotPassword = findViewById(R.id.TvForgotPassword);
        signup = findViewById(R.id.TvSignUp);

        login.setOnClickListener(v -> {
            String text_email = email.getText().toString().trim();
            String text_password = password.getText().toString().trim();

            if (validateLoginInput(text_email, text_password)) {
                loginUser(text_email, text_password);
            }
        });

        signup.setOnClickListener(v -> navigateTo(SignupActivity.class));

        forgotPassword.setOnClickListener(v -> navigateTo(ForgotPasswordActivity.class));
    }

    private boolean validateLoginInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            this.email.setError("Please enter your email address");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            this.email.setError("Please enter a valid email address");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            this.password.setError("Please enter your password");
            return false;
        }

        if (password.length() < 6) {
            this.password.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    navigateTo(MainActivity.class);
                })
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Invalid Email or Password!", Toast.LENGTH_SHORT).show());
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(LoginActivity.this, targetActivity);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_SHORT).show();
    }
}