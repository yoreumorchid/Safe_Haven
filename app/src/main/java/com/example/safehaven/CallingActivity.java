package com.example.safehaven;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
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
    private Switch locationSharing, callRecording;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private FirebaseAuth auth;
    private DatabaseReference contactsReference;
    private String emergencyContact1;
    private MediaRecorder mMediaRecorder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        // Initialize UI components
        emergencyCall = findViewById(R.id.IvCall);
        locationSharing = findViewById(R.id.SwLocShare);
        callRecording = findViewById(R.id.SwRecCall);

        // Load emergency contact
        auth = FirebaseAuth.getInstance();
        String userID = auth.getCurrentUser().getUid();
        contactsReference = FirebaseDatabase.getInstance()
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

        // Check and request necessary permissions
        checkAndRequestPermissions();

        emergencyCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ActivityCompat.checkSelfPermission(CallingActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    makeEmergencyCall();
                    if(locationSharing.isChecked()) {
                        shareLocation();
                    }
                    if(callRecording.isChecked()) {
                        recordCall();
                    }
                } else {
                    Toast.makeText(CallingActivity.this,
                            "Call permission is required to make emergency calls!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        locationSharing.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isChecked) {
                shareLocation();
            } else {
                Toast.makeText(this,
                        "Location sharing disabled",
                        Toast.LENGTH_SHORT).show();
            }
        }));

        callRecording.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isChecked) {
                recordCall();
            } else {
                Toast.makeText(this,
                        "Call recording disabled",
                        Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void checkAndRequestPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
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

    private void shareLocation() {
       Intent locationIntent = new Intent(this, MessageActivity.class);
       startActivity(locationIntent);
        Toast.makeText(this, "Sharing live location...", Toast.LENGTH_SHORT).show();
    }
    private boolean isRecording = false;
    private void recordCall() {
        if(isRecording) {
            stopRecoringCall();
            Toast.makeText(this,
                    "Call recording stopped",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if(mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
            }
            // Set audio source to voice call
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);

            // Set output format and encoder
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Set output file location
            String outputFilePath = getExternalFilesDir(null).
                    getAbsolutePath() + "/Safe_Haven_Call_Recording.3gp";
            mMediaRecorder.setOutputFile(outputFilePath);

            // Start recording
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;

            Toast.makeText(this,
                    "Call recording started",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Failed to start call recording: "
            + e.getMessage(),
                    Toast.LENGTH_SHORT).show();

            if(mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        }
    }

    private void stopRecoringCall() {
        if(mMediaRecorder != null && isRecording) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
                isRecording = false;
                Toast.makeText(this,
                        "Recording saved",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this,
                        "Error stopping recording: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }




}