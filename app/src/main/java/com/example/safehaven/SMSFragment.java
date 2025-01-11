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
    private FirebaseAuth auth;
    private DatabaseReference locationLogsReference;
    private ToggleButton locationSharingToggle;
    private static final long UPDATE_INTERVAL = 30000; // 30秒更新一次位置
    private static final int PERMISSION_REQUEST_CODE = 100;

    public SMSFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.SMSFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        auth = FirebaseAuth.getInstance();
        String userID = auth.getCurrentUser().getUid();
        locationLogsReference = FirebaseDatabase.getInstance().getReference("Users").child(userID).child("LocationLogs");

        // Initialise Location Manager
        mLocationManager = (LocationManager) requireContext().getSystemService(getContext().LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());
                String timestamp = String.valueOf(System.currentTimeMillis());

                // create Map
                Map<String, String> locationData = new HashMap<>();
                locationData.put("latitude", latitude);
                locationData.put("longitude", longitude);
                locationData.put("timestamp", timestamp);

                // save to Firebase LocationLogs node
                locationLogsReference.push().setValue(locationData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Location successfully recorded! Recording every 30 seconds.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to record location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        };


        locationSharingToggle = view.findViewById(R.id.TbLocationSharing);
        locationSharingToggle.setChecked(false);
        locationSharingToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (hasAllPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) {
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
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, mLocationListener);
        }
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
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
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
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
