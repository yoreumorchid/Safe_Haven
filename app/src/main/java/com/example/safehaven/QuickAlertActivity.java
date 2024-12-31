package com.example.safehaven;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.Toast;

public class QuickAlertActivity extends AppCompatActivity {

    private boolean isFlashlightOn = false; // Flashlight state
    private MediaPlayer sirenMediaPlayer; // MediaPlayer for siren

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_alert);

        // Get button references using updated IDs
        Button flashlightButton = findViewById(R.id.BtnFlashLight);
        Button loudSirenButton = findViewById(R.id.BtnLoudSiren);
        Button triggerButton = findViewById(R.id.BtnTrigger);

        // Flashlight SOS Signal Button
        flashlightButton.setOnClickListener(v -> toggleFlashlightSOS());

        // Loud Siren Playback Button
        loudSirenButton.setOnClickListener(v -> playLoudSiren());

        // One-Click Activation Button
        triggerButton.setOnClickListener(v -> {
            toggleFlashlightSOS();
            playLoudSiren();
        });
    }

    // Function to toggle Flashlight SOS
    private void toggleFlashlightSOS() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0]; // Access the first camera (usually back)
            isFlashlightOn = !isFlashlightOn;

            if (isFlashlightOn) {
                Toast.makeText(this, "Flashlight SOS Activated", Toast.LENGTH_SHORT).show();
                flashSOSPattern(cameraManager, cameraId); // Flash SOS pattern
            } else {
                cameraManager.setTorchMode(cameraId, false); // Turn off flashlight
                Toast.makeText(this, "Flashlight SOS Deactivated", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Unable to access flashlight", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to flash SOS pattern
    private void flashSOSPattern(CameraManager cameraManager, String cameraId) {
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) { // Flash 3 times
                    cameraManager.setTorchMode(cameraId, true);
                    Thread.sleep(300); // ON for 300ms
                    cameraManager.setTorchMode(cameraId, false);
                    Thread.sleep(300); // OFF for 300ms
                }
            } catch (CameraAccessException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Function to play loud siren
    private void playLoudSiren() {
        if (sirenMediaPlayer == null) {
            sirenMediaPlayer = MediaPlayer.create(this, R.raw.siren_sound); // Add siren_sound.mp3 in res/raw
        }

        if (!sirenMediaPlayer.isPlaying()) {
            sirenMediaPlayer.start();
            Toast.makeText(this, "Loud Siren Activated", Toast.LENGTH_SHORT).show();

            // Release MediaPlayer after playback
            sirenMediaPlayer.setOnCompletionListener(mp -> {
                sirenMediaPlayer.stop();
                sirenMediaPlayer.release();
                sirenMediaPlayer = null;
                Toast.makeText(this, "Loud Siren Deactivated", Toast.LENGTH_SHORT).show();
            });
        } else {
            sirenMediaPlayer.stop();
            sirenMediaPlayer.release();
            sirenMediaPlayer = null;
            Toast.makeText(this, "Loud Siren Deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Release MediaPlayer if the activity is destroyed
        if (sirenMediaPlayer != null) {
            sirenMediaPlayer.release();
            sirenMediaPlayer = null;
        }
        super.onDestroy();
    }
}