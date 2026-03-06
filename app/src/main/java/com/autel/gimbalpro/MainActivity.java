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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";
    private TextView statusTextView;
    private Button centerButton;
    private List<SeekBar> sliders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        if (statusTextView != null) {
            statusTextView.setSingleLine(false);
            statusTextView.setMaxLines(60);
            statusTextView.setTextSize(10);
            statusTextView.setText("Status: INITIALIZING L6 THERMAL SWEEP...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: L6 ARMED\n\nTAP 'CENTER ALL MOTORS' TO INITIATE THERMAL SWEEP.");
                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                    }
                });
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Handshake Failed: " + error.getDescription());
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeLevelSixScan());
        }
    }

    private void executeLevelSixScan() {
        try {
            StringBuilder report = new StringBuilder("=== L6 THERMAL SWEEP ===\n");
            
            Class<?> baseProductClass = Class.forName("com.autel.sdk.product.BaseProduct");
            
            // We know these are the component getter methods from our L2 scan
            String[] componentGetters = {"getGimbal", "getFlyController", "getRemoteController", "getCameraManager"};
            
            for (String getterName : componentGetters) {
                try {
                    Method getterMethod = baseProductClass.getMethod(getterName);
                    Class<?> componentClass = getterMethod.getReturnType();
                    
                    report.append("\n[Target: ").append(componentClass.getSimpleName()).append("]\n");
                    
                    boolean foundSomething = false;
                    for (Method m : componentClass.getMethods()) {
                        String name = m.getName().toLowerCase();
                        // The thermal filter: only show methods related to movement/angles
                        if (name.contains("angle") || name.contains("pitch") || 
                            name.contains("yaw") || name.contains("roll") || 
                            name.contains("move") || name.contains("rotation") || name.contains("dial")) {
                            
                            report.append("  HIT: ").append(m.getName()).append("(");
                            Class<?>[] params = m.getParameterTypes();
                            for (int i = 0; i < params.length; i++) {
                                report.append(params[i].getSimpleName());
                                if (i < params.length - 1) report.append(", ");
                            }
                            report.append(")\n");
                            foundSomething = true;
                        }
                    }
                    if (!foundSomething) {
                        report.append("  (No thermal signatures found)\n");
                    }
                    
                } catch (Exception e) {
                    report.append("  [-] Could not scan ").append(getterName).append("\n");
                }
            }

            if (statusTextView != null) {
                statusTextView.setText(report.toString());
                statusTextView.setTextColor(android.graphics.Color.parseColor("#000000"));
            }
        } catch (Exception e) {
            if (statusTextView != null) {
                statusTextView.setText("Scan Error: " + e.toString());
            }
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
