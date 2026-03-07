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

        updateStatus("Status: L-16 AMBUSH ARMED.\n\nWAITING FOR PHYSICAL USB HANDSHAKE...", android.graphics.Color.BLUE);

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
                        updateStatus("Status: DRONE DISCONNECTED.\nWaiting for re-link...", android.graphics.Color.RED);
                    }
                });
            }
            @Override
            public void onFailure(AutelError error) {
                updateStatus("SDK INIT ERROR: " + error.getDescription(), android.graphics.Color.RED);
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeFiringSequence());
        }
    }

    private void bindProduct(BaseProduct product) {
        liveDrone = product;
        try {
            Method getGimbalMethod = liveDrone.getClass().getMethod("getGimbal");
            liveGimbal = getGimbalMethod.invoke(liveDrone);
            updateStatus("HVT SECURED! TARGET LOCKED.\n\nAdjust sliders and fire.", android.graphics.Color.parseColor("#00AA00"));
        } catch (Exception e) {
            updateStatus("CONNECTION MADE, BUT GIMBAL EXTRACTION FAILED:\n" + e.getMessage(), android.graphics.Color.RED);
        }
    }

    private void executeFiringSequence() {
        if (liveGimbal == null) {
            updateStatus("MISFIRE: NO GIMBAL LOCK.", android.graphics.Color.RED);
            return;
        }
        try {
            int pitch = (pitchSlider != null) ? pitchSlider.getProgress() - 50 : 0;
            int roll = (rollSlider != null) ? rollSlider.getProgress() - 50 : 0;
            int yaw = (yawSlider != null) ? yawSlider.getProgress() - 50 : 0;

            Class<?> gimbalClass = liveGimbal.getClass();
            try {
                Method m = gimbalClass.getMethod("setGimbalAngle", int.class, int.class, int.class);
                m.invoke(liveGimbal, pitch, roll, yaw);
                updateStatus("SPLASH (INT): P=" + pitch + " R=" + roll + " Y=" + yaw, android.graphics.Color.parseColor("#00AA00"));
            } catch (Exception e) {
                Method m = gimbalClass.getMethod("setGimbalAngle", float.class, float.class, float.class);
                m.invoke(liveGimbal, (float)pitch, (float)roll, (float)yaw);
                updateStatus("SPLASH (FLOAT): P=" + pitch + " R=" + roll + " Y=" + yaw, android.graphics.Color.parseColor("#00AA00"));
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
                String text = ((Button) child).getText().toString().toUpperCase();
                if (text.contains("CENTER")) centerButton = (Button) child;
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
