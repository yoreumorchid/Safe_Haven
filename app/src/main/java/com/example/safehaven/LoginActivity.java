package com.example.safehaven;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
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

        // Initialize UI components
        email = findViewById(R.id.EtUsername);
        password = findViewById(R.id.EtPassword);
        login = findViewById(R.id.BtnLogin);
        forgotPassword = findViewById(R.id.TvForgotPassword);
        signup = findViewById(R.id.TvSignUp);
        auth = FirebaseAuth.getInstance();

        // Focus on the email field
        email.requestFocus();

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text_email = email.getText().toString().trim();
                String text_password = password.getText().toString().trim();

                if(validateLoginInput(text_email, text_password)) {
                    loginUser(text_email, text_password);
                }
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(SignupActivity.class);
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(ForgotPasswordActivity.class);
            }
        });
    }

    // Validate user input
    private boolean validateLoginInput(String email, String password) {
        if(TextUtils.isEmpty(email)) {
            this.email.setError("Please enter your email address");
            return false;
        }

        if(TextUtils.isEmpty(password)) {
            this.password.setError("Please enter your password");
            return false;
        }

        return true;
    }

    // Login user with Firebase
    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        navigateTo(MainActivity.class);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, "Invalid Email or Password!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(LoginActivity.this, targetActivity);
        startActivity(intent);
        finish();
    }
}