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
import java.util.ArrayList;
import java.util.List;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";
    private TextView statusTextView;
    private Button centerButton;

    // The live, captured HVT
    private BaseProduct liveDrone = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        if (statusTextView != null) {
            statusTextView.setSingleLine(false);
            statusTextView.setMaxLines(80);
            statusTextView.setTextSize(10);
            statusTextView.setText("Status: INITIALIZING L7 LIVE CAPTURE...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder().setPostOnUi(true).create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                // Plant the bug
                Autel.setProductConnectListener(new ProductConnectListener() {
                    @Override
                    public void productConnected(BaseProduct product) {
                        liveDrone = product; // Target Acquired
                        runOnUiThread(() -> {
                            if (statusTextView != null) {
                                statusTextView.setText("Status: HVT CAPTURED!\nLive Class: " + product.getClass().getName() + "\n\nTAP 'CENTER ALL MOTORS' TO INTERROGATE.");
                                statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                            }
                        });
                    }

                    @Override
                    public void productDisconnected() {
                        liveDrone = null;
                    }
                });
                
                runOnUiThread(() -> {
                    if (statusTextView != null && liveDrone == null) {
                        statusTextView.setText("Status: LISTENING POST ACTIVE.\n\nTURN DRONE ON TO CAPTURE HVT.");
                    }
                });
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Handshake Failed: " + error.getDescription());
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeLevelSevenScan());
        }
    }

    private void executeLevelSevenScan() {
        if (liveDrone == null) {
            if (statusTextView != null) statusTextView.setText("ERROR: NO HVT IN CUSTODY. TURN DRONE ON.");
            return;
        }

        try {
            StringBuilder report = new StringBuilder("=== L7 HVT INTERROGATION ===\n");
            
            // Extract the live Gimbal component from the captured drone
            Method getGimbalMethod = liveDrone.getClass().getMethod("getGimbal");
            Object liveGimbal = getGimbalMethod.invoke(liveDrone);
            
            if (liveGimbal != null) {
                Class<?> realGimbalClass = liveGimbal.getClass();
                report.append("REAL GIMBAL CLASS: ").append(realGimbalClass.getName()).append("\n\n");
                
                // Force dump the REAL methods from the obfuscated class
                for (Method m : realGimbalClass.getDeclaredMethods()) {
                    String name = m.getName();
                    // Filter out basic Java noise to save screen space
                    if (!name.contains("access$") && !name.equals("toString") && !name.equals("hashCode")) {
                        report.append(name).append("(");
                        Class<?>[] params = m.getParameterTypes();
                        for (int i=0; i<params.length; i++) {
                            report.append(params[i].getSimpleName());
                            if (i < params.length -1) report.append(",");
                        }
                        report.append(")\n");
                    }
                }
            } else {
                report.append("LIVE GIMBAL RETURNED NULL.");
            }

            if (statusTextView != null) {
                statusTextView.setText(report.toString());
                statusTextView.setTextColor(android.graphics.Color.parseColor("#000000"));
            }
        } catch (Exception e) {
            if (statusTextView != null) statusTextView.setText("Scan Error: " + e.toString());
        }
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
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
