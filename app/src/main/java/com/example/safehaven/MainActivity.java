package com.example.safehaven;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class  MainActivity extends AppCompatActivity {
    private Button profile, addContacts, emergencyCall, quickAlert, fakeDial;
    private ImageView emergencyMsg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Request necessary permissions
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.SEND_SMS, android.Manifest.permission.CALL_PHONE},
                PackageManager.PERMISSION_GRANTED);

        // Initialize UI components
        profile = findViewById(R.id.BtnProfile);
        addContacts = findViewById(R.id.BtnContacts);
        emergencyMsg = findViewById(R.id.IvMessage);
        emergencyCall = findViewById(R.id.BtnEmergencyCall);
        quickAlert = findViewById(R.id.BtnAlert);
        fakeDial = findViewById(R.id.BtnDial);

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