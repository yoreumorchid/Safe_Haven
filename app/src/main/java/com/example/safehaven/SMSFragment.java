package com.example.safehaven;

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

public class SMSFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap gMap;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Marker currentMarker;
    private Handler mHandler;
    private Runnable locationUpdateTask;
    private ToggleButton locationSharingToggle;
    private FirebaseAuth auth;
    private DatabaseReference contactsReference;
    private String emergencyContact1;
    private String currentLocationMessage = "Location unavailable.";
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds
    private static final int PERMISSION_REQUEST_CODE = 100;

    public SMSFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.SMSFragment);
        if(mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize location sharing switch
        locationSharingToggle = getView().findViewById(R.id.TbLocationSharing);
        locationSharingToggle.setChecked(false);
        locationSharingToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (hasAllPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS})) {
                    startLocatingUpdates();
                } else {
                    locationSharingToggle.setChecked(false);
                    requestPermissionsIfNeeded();
                }
            } else {
                stopLocationUpdates();
            }
        });


        mLocationManager = (LocationManager) requireContext().getSystemService(getContext().LOCATION_SERVICE);
        mHandler = new Handler();
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                currentLocationMessage = "I need help! My location is: "
                        + "http://maps.google.com/?q=" + currentLocation.latitude + "," + currentLocation.longitude;
                if(gMap != null) {
                    if(currentMarker != null) {
                        currentMarker.setPosition(currentLocation);
                    } else {
                        currentMarker = gMap.addMarker(new MarkerOptions().position(currentLocation).title("My Location"));
                    }
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                }
            }
        };

        // Request permissions
        requestPermissionsIfNeeded();

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
                    Toast.makeText(getContext(),
                            "No emergency contact found",
                            Toast.LENGTH_SHORT).show();
                    emergencyContact1 = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load emergency contact",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
    }

    private void startLocatingUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownLocation != null) {
            LatLng currentLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            currentLocationMessage = "I need help! My location is: "
                    + "http://maps.google.com/?q=" + currentLocation.latitude + "," + currentLocation.longitude;

            // Update Map
            if (gMap != null) {
                if (currentMarker != null) {
                    currentMarker.setPosition(currentLocation);
                } else {
                    currentMarker = gMap.addMarker(new MarkerOptions().position(currentLocation).title("My Location"));
                }
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10));
            }
        }

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL,
                0, mLocationListener);

        locationUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (emergencyContact1 != null && !currentLocationMessage.equals("Location unavailable.")) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(emergencyContact1, null, currentLocationMessage, null, null);
                    Toast.makeText(getContext(),
                            "Location shared via SMS",
                            Toast.LENGTH_SHORT).show();
                }
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        mHandler.post(locationUpdateTask);
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        if (mHandler != null && locationUpdateTask != null) {
            mHandler.removeCallbacks(locationUpdateTask);
        }
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
        };

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(getContext(), "Permissions are required for this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocationUpdates();
    }


}