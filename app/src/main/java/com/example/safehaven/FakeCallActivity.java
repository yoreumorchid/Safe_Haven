package com.example.safehaven;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FakeCallActivity extends AppCompatActivity {

    private RadioGroup rgVoiceSelection;
    private Button swHaptics;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);
        rgVoiceSelection = findViewById(R.id.Rg_voice_selection);
        swHaptics = findViewById(R.id.SWHaptics);


        // Set click listener for the Dial button
        swHaptics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check which radio button is selected
                int selectedId = rgVoiceSelection.getCheckedRadioButtonId();
                if (selectedId == R.id.Rb_Female) {
                    // Female voice selected
                    playVoice(R.raw.female_voice);
                } else if (selectedId == R.id.Rb_male) {
                    // Male voice selected
                    playVoice(R.raw.male_voice);
                } else {
                    // No option selected
                    Toast.makeText(FakeCallActivity.this, "Please select a voice type first!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to play the selected voice
    private void playVoice(int voiceResId) {
        // Stop any previous playback
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        // Initialize MediaPlayer with the selected audio file
        mediaPlayer = MediaPlayer.create(this, voiceResId);
        mediaPlayer.start();

        // Release MediaPlayer resources when playback completes
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release MediaPlayer if it's still active
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
