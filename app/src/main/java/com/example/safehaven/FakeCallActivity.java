package com.example.safehaven;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safehaven.R;


public class FakeCallActivity extends AppCompatActivity {

    private RadioGroup rgVoiceSelection; // RadioGroup for selecting voice type
    private Switch swVoice;             // Switch to control voice playback
    private Switch swVibration;         // Switch to control vibration
    private MediaPlayer mediaPlayer;    // MediaPlayer for playing voice
    private Vibrator vibrator;          // Vibrator object for handling vibrations

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);
        //TODO: Add logic to deal with the situation that user decline the vibration permission
        rgVoiceSelection = findViewById(R.id.Rg_voice_selection);
        swVoice = findViewById(R.id.swVoice);
        swVibration = findViewById(R.id.swVibration); // Initialize vibration control Switch

        // Initialize the vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Set a listener for the vibration control Switch
        swVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Enable vibration
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(effect);
                    } else {
                        vibrator.vibrate(500); // Vibrate for 500 milliseconds
                    }
                    Toast.makeText(this, "Vibration enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Device does not support vibration", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Disable vibration
                if (vibrator != null) {
                    vibrator.cancel();
                }
                Toast.makeText(this, "Vibration disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Set a listener for the voice playback Switch
        swVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // When the switch is ON: play the selected voice
                int selectedId = rgVoiceSelection.getCheckedRadioButtonId();
                if (selectedId == R.id.Rb_Female) {
                    playVoice(R.raw.female_voice);
                } else if (selectedId == R.id.Rb_male) {
                    playVoice(R.raw.male_voice);
                } else {
                    // If no voice type is selected
                    Toast.makeText(FakeCallActivity.this, "Please select a voice type first!", Toast.LENGTH_SHORT).show();
                    swVoice.setChecked(false);
                }
            } else {
                // When the switch is OFF: stop playback
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    Toast.makeText(FakeCallActivity.this, "Playback stopped", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to play the selected voice
    private void playVoice(int voiceResId) {
        // Stop any existing playback
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        // Initialize MediaPlayer with the selected audio file
        mediaPlayer = MediaPlayer.create(this, voiceResId);
        mediaPlayer.start();
        // Release MediaPlayer resources when playback completes
        mediaPlayer.setOnCompletionListener(mp -> {
            mediaPlayer.release();
            mediaPlayer = null;
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release MediaPlayer resources if still active
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // Cancel vibration if the activity is destroyed
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
