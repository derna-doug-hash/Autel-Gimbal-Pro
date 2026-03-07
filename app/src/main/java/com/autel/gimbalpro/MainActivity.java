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
import com.autel.sdk.ProductConnectListener;
import com.autel.sdk.product.BaseProduct;

import java.lang.reflect.Method;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";
    private TextView statusTextView;
    private Button centerButton;
    private SeekBar pitchSlider, yawSlider, rollSlider;
    
    private BaseProduct liveDrone = null;
    private Object liveGimbal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        if (statusTextView != null) {
            statusTextView.setSingleLine(false);
            statusTextView.setMaxLines(80);
            statusTextView.setTextSize(12);
            statusTextView.setText("Status: OPERATION BLUEPRINT ARMED.\n\nWAITING FOR DRONE CONNECTION...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder().setPostOnUi(true).create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Autel.setProductConnectListener(new ProductConnectListener() {
                    @Override
                    public void productConnected(BaseProduct product) {
                        liveDrone = product;
                        try {
                            Method getGimbalMethod = liveDrone.getClass().getMethod("getGimbal");
                            liveGimbal = getGimbalMethod.invoke(liveDrone);
                            
                            runOnUiThread(() -> {
                                if (statusTextView != null) {
                                    statusTextView.setText("Status: HVT SECURED!\nTarget Locked. \n\nADJUST SLIDERS AND TAP 'CENTER ALL MOTORS' TO FIRE.");
                                    statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                                }
                            });
                        } catch (Exception e) {
                            logError("Gimbal Extraction Failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void productDisconnected() {
                        liveDrone = null;
                        liveGimbal = null;
                        logError("Drone Disconnected.");
                    }
                });
            }

            @Override
            public void onFailure(AutelError error) {
                logError("SDK Init Failed: " + error.getDescription());
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeFiringSequence());
        }
    }

    private void executeFiringSequence() {
        if (liveGimbal == null) {
            logError("NO GIMBAL TARGET ACQUIRED.");
            return;
        }

        try {
            // Read values from your sliders (assuming 0-100 range, centering at 0 for pitch/roll/yaw)
            // Adjust math here if your sliders have a different range setup!
            int pitch = (pitchSlider != null) ? pitchSlider.getProgress() - 50 : 0;
            int roll = (rollSlider != null) ? rollSlider.getProgress() - 50 : 0;
            int yaw = (yawSlider != null) ? yawSlider.getProgress() - 50 : 0;

            if (statusTextView != null) {
                statusTextView.setText("FIRING COORDINATES: P=" + pitch + " R=" + roll + " Y=" + yaw + "...");
                statusTextView.setTextColor(android.graphics.Color.RED);
            }

            // The Golden BB: Invoking the hidden method we found in the blueprints
            Class<?> gimbalClass = liveGimbal.getClass();
            
            // Try integer parameters first (as seen in the GitHub code)
            try {
                Method setAngleMethod = gimbalClass.getMethod("setGimbalAngle", int.class, int.class, int.class);
                setAngleMethod.invoke(liveGimbal, pitch, roll, yaw);
                
                if (statusTextView != null) {
                    statusTextView.setText("Status: SPLASH! INT COMMAND SENT.\n(P=" + pitch + ", R=" + roll + ", Y=" + yaw + ")");
                    statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                }
            } catch (NoSuchMethodException e) {
                // If it's not integers, Autel might have switched to floats in the V3 update
                Method setAngleMethodFloat = gimbalClass.getMethod("setGimbalAngle", float.class, float.class, float.class);
                setAngleMethodFloat.invoke(liveGimbal, (float)pitch, (float)roll, (float)yaw);
                
                if (statusTextView != null) {
                    statusTextView.setText("Status: SPLASH! FLOAT COMMAND SENT.\n(P=" + pitch + ", R=" + roll + ", Y=" + yaw + ")");
                    statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                }
            }

        } catch (Exception e) {
            logError("Misfire: " + e.toString());
        }
    }

    private void logError(String msg) {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText("ERROR: " + msg);
                statusTextView.setTextColor(android.graphics.Color.RED);
            }
        });
    }

    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button) && !(child instanceof Switch)) {
                String text = ((TextView) child).getText().toString().toUpperCase();
                if (text.contains("STATUS") || text.contains("UNKNOWN")) statusTextView = (TextView) child;
            } else if (child instanceof Button) {
                String text = ((Button) child).getText().toString().toUpperCase();
                if (text.contains("CENTER")) centerButton = (Button) child;
            } else if (child instanceof SeekBar) {
                // Assigning the sliders based on the order they appear in the UI
                if (pitchSlider == null) pitchSlider = (SeekBar) child;
                else if (yawSlider == null) yawSlider = (SeekBar) child;
                else if (rollSlider == null) rollSlider = (SeekBar) child;
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
