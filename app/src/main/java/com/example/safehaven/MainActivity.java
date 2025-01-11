package com.example.safehaven;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.Manifest;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class  MainActivity extends AppCompatActivity {
    private Button profile, addContacts, emergencyCall, quickAlert, fakeDial, logout;
    private FirebaseAuth auth;
    private ImageView emergencyMsg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Request necessary permissions
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.SEND_SMS, android.Manifest.permission.CALL_PHONE},
                PackageManager.PERMISSION_GRANTED);

        // Check if the user is logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        Log.d("LoginStatus", "isLoggedIn: " + isLoggedIn);

        if (!isLoggedIn) {
            navigateToActivity(LoginActivity.class);
            finish();
            return;
        }

        // Initialize UI components
        profile = findViewById(R.id.BtnProfile);
        addContacts = findViewById(R.id.BtnContacts);
        emergencyMsg = findViewById(R.id.IvMessage);
        emergencyCall = findViewById(R.id.BtnEmergencyCall);
        quickAlert = findViewById(R.id.BtnAlert);
        fakeDial = findViewById(R.id.BtnDial);
        logout = findViewById(R.id.BtnLogOut2);
        auth = FirebaseAuth.getInstance();

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(ProfileActivity.class);
            }
        });

        emergencyCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(CallingActivity.class);
            }
        });

        addContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(AddContactsActivity.class);
            }
        });

        emergencyMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToFragment(new SMSFragment());
            }
        });

        quickAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(QuickAlertActivity.class);
            }
        });

        fakeDial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(FakeCallActivity.class);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear login status in SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", false);  // Set it to 'false' when logged out
                editor.apply();  // Apply the changes

                // Navigate back to LoginActivity
                navigateToActivity(LoginActivity.class);
            }
        });
    }

    private void navigateToActivity(Class<?> targetActivity) {
        try {
            startActivity(new Intent(MainActivity.this, targetActivity));
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToFragment(Fragment targetFragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, targetFragment);
            fragmentTransaction.addToBackStack(null); // Optional
            fragmentTransaction.commit();
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}