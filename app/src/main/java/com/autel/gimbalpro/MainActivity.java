package com.autel.gimbalpro;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.autel.sdk.GimbalManager;
import com.autel.sdk.product.BaseProduct;

public class MainActivity extends AppCompatActivity {
    private GimbalManager gimbalManager;
    private boolean isRollInverted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch invertSwitch = findViewById(R.id.rollInvertSwitch);
        SeekBar rollBar = findViewById(R.id.rollSeekBar);

        invertSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRollInverted = isChecked;
        });

        rollBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float angle = progress - 30; // Range -30 to +30
                    if (isRollInverted) { angle = -angle; } // THE FIX: Manual logic reversal
                    sendGimbalCommand(0, 0, angle);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void sendGimbalCommand(float pitch, float yaw, float roll) {
        // Simplified SDK call to target the V3 Gimbal directly
        if (gimbalManager != null) {
            gimbalManager.setGimbalAngle(pitch, roll, yaw);
        }
    }
}
