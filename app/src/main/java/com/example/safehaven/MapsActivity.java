package com.example.safehaven;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapsActivity extends AppCompatActivity {
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private FirebaseAuth auth;
    private Handler mHandler;
    private Runnable locationUpdateTask;
    private final long UPDATE_INTERVAL = 30000;
    private final float MIN_DISTANCE = 10;
    private String[] emergencyContacts = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        auth = FirebaseAuth.getInstance();
        String userID = auth.getCurrentUser().getUid();

        // Retrieve emergency contacts
        DatabaseReference reference = FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(userID)
                .child("Contacts");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Contacts contacts = snapshot.getValue(Contacts.class);
                if(contacts != null) {
                    emergencyContacts[0] = contacts.contact1.trim();
                    emergencyContacts[1] = contacts.contact2.trim();
                    emergencyContacts[2] = contacts.contact3.trim();
                    emergencyContacts[3] = contacts.contact4.trim();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this,
                        "Failed to load contacts",
                        Toast.LENGTH_SHORT).show();
            }
        });

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                PackageManager.PERMISSION_GRANTED);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                sendLocationToContacts(location);
            }
        };

        // Start location sharing with contacts
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    UPDATE_INTERVAL,
                    MIN_DISTANCE,
                    mLocationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Initialize periodic location sharing
        mHandler = new Handler();
        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if(lastKnownLocation != null) {
                        sendLocationToContacts(lastKnownLocation);
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        mHandler.post(locationUpdateTask);
    }

    private void sendLocationToContacts (Location location) {
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        String message = "I need help! My current location is http://maps.google.com/?q=" + latitude + "," + longitude;

        SmsManager smsManager = SmsManager.getDefault();
        for (String contact : emergencyContacts) {
            if (contact != null && !contact.isEmpty()) {
                smsManager.sendTextMessage(contact, null, message, null, null);
            }
        }

        Toast.makeText(this,
                "Location sent to emergency contacts",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacks(locationUpdateTask);
        }
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }
}