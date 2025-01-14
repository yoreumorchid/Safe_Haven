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

import java.util.ArrayList;
import java.util.List;

public class CallingActivity extends AppCompatActivity {
    private ImageView emergencyCall;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private FirebaseAuth auth;
    private DatabaseReference contactsReference;
    private String emergencyContact1;
    private boolean isFirstLocationUpdate = true;

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

        locationSharing.setChecked(true);

        // Initialize Phone Listener
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    stopLocationUpdates();
                }
            }
        };

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
                    Toast.makeText(CallingActivity.this, "No emergency contact found", Toast.LENGTH_SHORT).show();
                    emergencyContact1 = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CallingActivity.this, "Failed to load emergency contact", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize location sharing
        initializeLocationSharing();

        // Check and request necessary permissions
        checkAndRequestPermissions();

        emergencyCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(CallingActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    makeEmergencyCall();

                    if (locationSharing.isChecked()) {
                        startLocationUpdates();
                    }

                } else {
                    ActivityCompat.requestPermissions(CallingActivity.this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CODE);
                }
            }
        });
    }

    private void initializeLocationSharing() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mHandler = new Handler();
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocationMessage = "I need help! My current location is: "
                        + "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();

                if (isFirstLocationUpdate) {
                    isFirstLocationUpdate = false;
                    startLocationUpdatesTask();
                }
            }
        };
    }

    private void startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null) {
                    currentLocationMessage = "I need help! My current location is: "
                            + "http://maps.google.com/?q=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
                    isFirstLocationUpdate = false;
                    startLocationUpdatesTask();
                }

                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE,
                        mLocationListener);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void startLocationUpdatesTask() {
        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (emergencyContact1 != null && !currentLocationMessage.equals("Location unavailable.")) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(emergencyContact1, null, currentLocationMessage, null, null);
                    Toast.makeText(CallingActivity.this, "Location shared.", Toast.LENGTH_SHORT).show();
                }
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        mHandler.postDelayed(locationUpdateTask, 5000);
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        if (mHandler != null && locationUpdateTask != null) {
            mHandler.removeCallbacks(locationUpdateTask);
        }
    }

    private void checkAndRequestPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            registerPhoneStateListener();
        }
    }

    private void registerPhoneStateListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            boolean shouldShowRationale = false;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        shouldShowRationale = true;
                    }
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                registerPhoneStateListener();
            } else if (shouldShowRationale) {
                showPermissionExplanationDialog();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app requires permissions to function properly. Please grant the necessary permissions.")
                .setPositiveButton("Retry", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showPermissionDeniedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("You have permanently denied some permissions. Please enable them in app settings or exit the app.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }


    private void makeEmergencyCall() {
        if (emergencyContact1 != null && !emergencyContact1.isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + emergencyContact1));
            startActivity(callIntent);
        } else {
            Toast.makeText(this, "No emergency contact to call", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(CallingActivity.this, MainActivity.class));
        finish();
    }
}