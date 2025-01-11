package com.example.safehaven;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SMSFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap gMap;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Marker currentMarker;
    private Handler mHandler;
    private Runnable locationUpdateTask;
    private ToggleButton locationSharingToggle;
    private FirebaseAuth auth;
    private DatabaseReference locationLogsReference;
    private DatabaseReference contactsReference;
    private String emergencyContact1;
    private String currentLocationMessage = "Location unavailable.";
    private boolean isMapReady = false;
    private boolean shouldZoomOnMapReady = false;
    private static final long UPDATE_INTERVAL = 30000;
    private static final int PERMISSION_REQUEST_CODE = 100;

    public SMSFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.SMSFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        auth = FirebaseAuth.getInstance();
        String userID = auth.getCurrentUser().getUid();
        locationLogsReference = FirebaseDatabase.getInstance().getReference("Users").child(userID).child("LocationLogs");
        contactsReference = FirebaseDatabase.getInstance().getReference("Users").child(userID).child("Contacts");

        mLocationManager = (LocationManager) requireContext().getSystemService(getContext().LOCATION_SERVICE);
        mHandler = new Handler();

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());
                String timestamp = String.valueOf(System.currentTimeMillis());

                currentLocationMessage = "I need help! My location is: " +
                        "http://maps.google.com/?q=" + latitude + "," + longitude;

                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (gMap != null && isMapReady) {
                    if (currentMarker != null) {
                        currentMarker.setPosition(currentLocation);
                    } else {
                        currentMarker = gMap.addMarker(new MarkerOptions().position(currentLocation).title("My Location"));
                    }
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                } else {
                    shouldZoomOnMapReady = true;
                }

                Map<String, String> locationData = new HashMap<>();
                locationData.put("latitude", latitude);
                locationData.put("longitude", longitude);
                locationData.put("timestamp", timestamp);
                locationLogsReference.push().setValue(locationData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Location successfully recorded!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to record location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                if (emergencyContact1 != null) {
                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(emergencyContact1, null, currentLocationMessage, null, null);
                        Toast.makeText(getContext(), "Location shared via SMS", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        locationSharingToggle = view.findViewById(R.id.TbLocationSharing);
        locationSharingToggle.setChecked(false);
        locationSharingToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (hasAllPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS})) {
                    startLocationUpdates();
                } else {
                    locationSharingToggle.setChecked(false);
                    requestPermissionsIfNeeded();
                }
            } else {
                stopLocationUpdates();
            }
        });

        Button viewLocationLogsButton = view.findViewById(R.id.btnViewLocationLogs);
        viewLocationLogsButton.setOnClickListener(v -> showLocationLogsDialog());

        contactsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Contacts contacts = snapshot.getValue(Contacts.class);
                if (contacts != null && contacts.contact1 != null && !contacts.contact1.isEmpty()) {
                    emergencyContact1 = contacts.contact1.trim();
                } else {
                    Toast.makeText(getContext(), "No emergency contact found", Toast.LENGTH_SHORT).show();
                    emergencyContact1 = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load emergency contact", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        isMapReady = true;
        if (shouldZoomOnMapReady && currentMarker != null) {
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMarker.getPosition(), 15));
            shouldZoomOnMapReady = false;
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownLocation != null) {
            LatLng currentLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            currentLocationMessage = "I need help! My location is: " +
                    "http://maps.google.com/?q=" + currentLocation.latitude + "," + currentLocation.longitude;

            if (gMap != null && isMapReady) {
                if (currentMarker != null) {
                    currentMarker.setPosition(currentLocation);
                } else {
                    currentMarker = gMap.addMarker(new MarkerOptions().position(currentLocation).title("My Location"));
                }
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            } else {
                shouldZoomOnMapReady = true;
            }
        }

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, mLocationListener);
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        if (mHandler != null && locationUpdateTask != null) {
            mHandler.removeCallbacks(locationUpdateTask);
        }
    }

    private void showLocationLogsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Location Logs");

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout logContainer = new LinearLayout(getContext());
        logContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(logContainer);

        locationLogsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                        Map<String, String> logEntry = (Map<String, String>) logSnapshot.getValue();
                        if (logEntry != null) {
                            String logText = "Lat: " + logEntry.get("latitude") + ", Lon: " + logEntry.get("longitude") + ", Time: " + logEntry.get("timestamp");
                            TextView logTextView = new TextView(getContext());
                            logTextView.setText(logText);
                            logTextView.setPadding(10, 10, 10, 10);
                            logContainer.addView(logTextView);
                        }
                    }
                } else {
                    TextView emptyView = new TextView(getContext());
                    emptyView.setText("No location logs found.");
                    emptyView.setPadding(10, 10, 10, 10);
                    logContainer.addView(emptyView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load location logs.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(scrollView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS};
        if (!hasAllPermissions(permissions)) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasAllPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocationUpdates();
    }
}
