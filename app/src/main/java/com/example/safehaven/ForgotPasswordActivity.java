package com.example.safehaven;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText email;
    private Button resetPasswordConfirm;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        email = findViewById(R.id.EtResetPasswordEmail);
        resetPasswordConfirm = findViewById(R.id.BtnResetPasswordConfirm);
        auth = FirebaseAuth.getInstance();

        resetPasswordConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePasswordReset();
            }
        });
    }

    // Handle password reset login
    private void handlePasswordReset() {
        String textEmail = email.getText().toString().trim();
        if(!validateEmail(textEmail)) return;

        // Disable button to prevent multiple clicks
        resetPasswordConfirm.setEnabled(false);
        auth.sendPasswordResetEmail(textEmail)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        resetPasswordConfirm.setText("RESEND RESET PASSWORD EMAIL");
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Check your mailbox to reset your password",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Please provide a valid email address",
                                Toast.LENGTH_SHORT).show();
                    }
                    resetPasswordConfirm.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Something went wrong. Try again!",
                            Toast.LENGTH_SHORT).show();
                    resetPasswordConfirm.setEnabled(true);
                });
    }

    private boolean validateEmail(String emailInput) {
        if(TextUtils.isEmpty(emailInput)) {
            email.setError("Please enter your email");
            return false;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()){
            email.setError("Please provide a valid email address");
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
        finish();
    }
}