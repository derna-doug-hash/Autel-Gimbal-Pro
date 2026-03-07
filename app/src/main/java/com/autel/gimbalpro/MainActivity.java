package com.autel.gimbalpro;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;
import com.autel.sdk.ProductConnectListener;
import com.autel.sdk.product.BaseProduct;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
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

        updateStatus("Status: L-14.2 STEALTH PROBE ARMED.\n\nCLEAR DEFAULTS, THEN TAP 'CENTER' TO SNATCH CONNECTION.", android.graphics.Color.BLUE);

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder().setPostOnUi(true).create();
        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Autel.setProductConnectListener(new ProductConnectListener() {
                    @Override
                    public void productConnected(BaseProduct product) {
                        bindProduct(product);
                    }
                    @Override
                    public void productDisconnected() {
                        liveDrone = null;
                        liveGimbal = null;
                        updateStatus("Drone Disconnected.", android.graphics.Color.RED);
                    }
                });
            }
            @Override
            public void onFailure(AutelError error) {
                updateStatus("SDK Error: " + error.getDescription(), android.graphics.Color.RED);
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> {
                if (liveDrone == null) {
                    stealthProbe();
                } else {
                    executeFiringSequence();
                }
            });
        }
    }

    private void stealthProbe() {
        updateStatus("INITIATING STEALTH PROBE...", android.graphics.Color.MAGENTA);
        try {
            // We are going to try to find the 'mCurrentProduct' field inside the Autel class
            // which is where most versions of this SDK store the live connection.
            Field productField = Autel.class.getDeclaredField("mCurrentProduct");
            productField.setAccessible(true);
            BaseProduct product = (BaseProduct) productField.get(null);

            if (product != null) {
                bindProduct(product);
                updateStatus("STEALTH SNATCH SUCCESSFUL!", android.graphics.Color.parseColor("#00AA00"));
            } else {
                updateStatus("PROBE FAILED: NO PRODUCT IN MEMORY.\nEnsure drone is linked to controller.", android.graphics.Color.RED);
            }
        } catch (Exception e) {
            updateStatus("PROBE ERROR: SDK MEMORY PROTECTED.\n" + e.getMessage(), android.graphics.Color.RED);
        }
    }

    private void bindProduct(BaseProduct product) {
        liveDrone = product;
        try {
            Method getGimbalMethod = liveDrone.getClass().getMethod("getGimbal");
            liveGimbal = getGimbalMethod.invoke(liveDrone);
            updateStatus("HVT SECURED! TARGET LOCKED.\nFire coordinates now.", android.graphics.Color.parseColor("#00AA00"));
        } catch (Exception e) {
            updateStatus("Gimbal Extraction Failed: " + e.getMessage(), android.graphics.Color.RED);
        }
    }

    private void executeFiringSequence() {
        if (liveGimbal == null) return;
        try {
            int pitch = (pitchSlider != null) ? pitchSlider.getProgress() - 50 : 0;
            int roll = (rollSlider != null) ? rollSlider.getProgress() - 50 : 0;
            int yaw = (yawSlider != null) ? yawSlider.getProgress() - 50 : 0;

            Class<?> gimbalClass = liveGimbal.getClass();
            try {
                Method m = gimbalClass.getMethod("setGimbalAngle", int.class, int.class, int.class);
                m.invoke(liveGimbal, pitch, roll, yaw);
                updateStatus("SPLASH (INT): P=" + pitch, android.graphics.Color.parseColor("#00AA00"));
            } catch (Exception e) {
                Method m = gimbalClass.getMethod("setGimbalAngle", float.class, float.class, float.class);
                m.invoke(liveGimbal, (float)pitch, (float)roll, (float)yaw);
                updateStatus("SPLASH (FLOAT): P=" + pitch, android.graphics.Color.parseColor("#00AA00"));
            }
        } catch (Exception e) {
            updateStatus("MISFIRE: " + e.getMessage(), android.graphics.Color.RED);
        }
    }

    private void updateStatus(String msg, int color) {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText(msg);
                statusTextView.setTextColor(color);
            }
        });
    }

    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button)) {
                if (child.getId() != View.NO_ID) statusTextView = (TextView) child;
            } else if (child instanceof Button) {
                centerButton = (Button) child;
            } else if (child instanceof SeekBar) {
                if (pitchSlider == null) pitchSlider = (SeekBar) child;
                else if (yawSlider == null) yawSlider = (SeekBar) child;
                else if (rollSlider == null) rollSlider = (SeekBar) child;
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
