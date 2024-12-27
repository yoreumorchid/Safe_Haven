package com.example.safehaven;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CallingActivity extends AppCompatActivity {
    private ImageView emergencyCall;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private FirebaseAuth auth;
    private DatabaseReference contactsReference;
    private String emergencyContact1;

    // Location sharing
    private Switch locationSharing;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Handler mHandler;
    private String currentLocationMessage = "Location unavailable.";
    private Runnable locationUpdateTask;
    private final long UPDATE_INTERVAL = 30000;
    private final float MIN_DISTANCE = 10;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        // Initialize UI components
        emergencyCall = findViewById(R.id.IvCall);
        locationSharing = findViewById(R.id.SwLocShare);

        // Set default states for the toggles
        locationSharing.setChecked(true);

        // Initialize Phone Listener
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                if(state == TelephonyManager.CALL_STATE_IDLE) {
                    stopLocationUpdates();
                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // Load emergency contact
        auth = FirebaseAuth.getInstance();
        String userID = auth.getCurrentUser().getUid();
        contactsReference = FirebaseDatabase.getInstance("https://safe-haven-38678-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(userID)
                .child("Contacts");

        contactsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Contacts contacts = snapshot.getValue(Contacts.class);
                if (contacts != null && contacts.contact1 != null && !contacts.contact1.isEmpty()) {
                    emergencyContact1 = contacts.contact1.trim();
                } else {
                    Toast.makeText(CallingActivity.this,
                            "No emergency contact found",
                            Toast.LENGTH_SHORT).show();
                    emergencyContact1 = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CallingActivity.this,
                        "Failed to load emergency contact",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize location sharing
        initializeLocationSharing();

        // Check and request necessary permissions
        checkAndRequestPermissions();

        emergencyCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ActivityCompat.checkSelfPermission(CallingActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    makeEmergencyCall();

                    if(locationSharing.isChecked()) {
                        startLocationUpdates();
                    }

                } else {
                    Toast.makeText(CallingActivity.this,
                            "Call permission is required to make emergency calls!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Location sharing
    // After make the emergency call, user needs to back to the CallingActivity to share location
    private void initializeLocationSharing() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mHandler = new Handler();
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocationMessage = "I need help! My current location is: "
                        + "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
            }
        };

        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                if(emergencyContact1 != null) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(emergencyContact1, null, currentLocationMessage, null, null);
                    Toast.makeText(CallingActivity.this,
                            "Location shared.",
                            Toast.LENGTH_SHORT).show();
                }
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    private void startLocationUpdates() {
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    UPDATE_INTERVAL,
                    MIN_DISTANCE,
                    mLocationListener);
            mHandler.post(locationUpdateTask);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        if (mHandler != null && locationUpdateTask != null) {
            mHandler.removeCallbacks(locationUpdateTask);
        }
    }

    // Permission request
    private void checkAndRequestPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };

        if (!hasAllPermissions(requiredPermissions)) {
            // Request missing permissions
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasAllPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "All permissions are required to proceed!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this,
                    "Permissions granted!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void makeEmergencyCall() {
        if (emergencyContact1 != null && !emergencyContact1.isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel: " + emergencyContact1));
            startActivity(callIntent);
        } else {
            Toast.makeText(this,
                    "No emergency contact to call",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }



}