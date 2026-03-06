package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;

import java.util.ArrayList;
import java.util.List;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";
    
    // UI Elements
    private TextView statusTextView;
    private Button centerButton;
    private Switch reverseLogicSwitch;
    private List<SeekBar> sliders = new ArrayList<>();
    
    // Gimbal Data States
    private boolean isReverseLogicEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Scan the screen and grab the controls
        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        if (statusTextView != null) {
            statusTextView.setText("Status: WIRING SLIDERS...");
        }

        // 2. Hook up the controls
        setupControls();

        // 3. Keep our stable Handshake
        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: SYSTEM ARMED - READY FOR GIMBAL INPUT");
                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                    }
                });
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Autel SDK Handshake Failed: " + error.getDescription());
            }
        });
    }

    private void setupControls() {
        // Wire the Reverse Logic Switch
        if (reverseLogicSwitch != null) {
            reverseLogicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isReverseLogicEnabled = isChecked;
                updateStatus("Reverse Logic: " + (isChecked ? "ON" : "OFF"));
            });
        }

        // Wire the Center Motors Button
        if (centerButton != null) {
            centerButton.setOnClickListener(v -> {
                // Reset sliders to middle (assuming 0-100 range, middle is 50)
                for (SeekBar slider : sliders) {
                    slider.setProgress(50);
                }
                updateStatus("COMMAND: CENTER ALL MOTORS");
            });
        }

        // Wire the 3 SeekBars (Pitch, Yaw, Roll)
        for (int i = 0; i < sliders.size(); i++) {
            final int sliderIndex = i;
            sliders.get(i).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        String axis = (sliderIndex == 0) ? "PITCH" : (sliderIndex == 1) ? "YAW" : "ROLL";
                        
                        // Apply Reverse Logic to Roll if enabled
                        int calculatedProgress = progress;
                        if (axis.equals("ROLL") && isReverseLogicEnabled) {
                            calculatedProgress = seekBar.getMax() - progress;
                        }
                        
                        updateStatus(axis + " Input: " + calculatedProgress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void updateStatus(String message) {
        if (statusTextView != null) {
            statusTextView.setText("Status: " + message);
        }
        Log.d(TAG, message);
    }

    // Bulletproof dynamic UI finder
    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            
            if (child instanceof TextView && !(child instanceof Button) && !(child instanceof Switch)) {
                String text = ((TextView) child).getText().toString().toUpperCase();
                if (text.contains("STATUS") || text.contains("UNKNOWN")) {
                    statusTextView = (TextView) child;
                }
            } else if (child instanceof Button) {
                String text = ((Button) child).getText().toString().toUpperCase();
                if (text.contains("CENTER")) {
                    centerButton = (Button) child;
                }
            } else if (child instanceof Switch) {
                reverseLogicSwitch = (Switch) child;
            } else if (child instanceof SeekBar) {
                sliders.add((SeekBar) child);
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Autel.destroy();
    }
}
