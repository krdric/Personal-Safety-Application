package com.example.sos;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class FakeCallActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private Vibrator vibrator;
    private TextView tvCallStatus, tvCallTimer, tvDecline, tvAccept;
    private FloatingActionButton btnDecline, btnAccept;
    private LinearLayout llCallControls;
    private View topGradient;
    private boolean isCallActive = false;
    private int secondsElapsed = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        btnDecline = findViewById(R.id.btn_decline);
        btnAccept = findViewById(R.id.btn_accept);
        tvCallStatus = findViewById(R.id.call_status);
        tvCallTimer = findViewById(R.id.tv_call_timer);
        tvDecline = findViewById(R.id.tv_decline);
        tvAccept = findViewById(R.id.tv_accept);
        llCallControls = findViewById(R.id.ll_call_controls);
        topGradient = findViewById(R.id.top_gradient);

        // Play default ringtone
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        if (ringtone != null) {
            ringtone.play();
        }

        // Start vibration
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000}; // Wait 0ms, vibrate 1000ms, sleep 1000ms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 means repeat
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAll();
                finish();
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptCall();
            }
        });
    }

    private void acceptCall() {
        stopAll();
        isCallActive = true;
        
        // Update UI
        tvCallStatus.setText("Ongoing call");
        tvCallTimer.setVisibility(View.VISIBLE);
        llCallControls.setVisibility(View.VISIBLE);
        
        // Hide initial buttons
        btnAccept.setVisibility(View.GONE);
        tvAccept.setVisibility(View.GONE);
        findViewById(R.id.textView_remind).setVisibility(View.GONE);
        findViewById(R.id.textView_message).setVisibility(View.GONE);
        
        // Reposition decline button as end call
        btnDecline.setRotation(0); // Reset rotation to look like end call
        
        // Start Timer
        startTimer();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                int minutes = secondsElapsed / 60;
                int secs = secondsElapsed % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
                tvCallTimer.setText(time);
                secondsElapsed++;
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopAll() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        stopAll();
        super.onDestroy();
    }
}
