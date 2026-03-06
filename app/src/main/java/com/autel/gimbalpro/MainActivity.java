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
    private Switch reverseLogicSwitch;
    private List<SeekBar> sliders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        if (statusTextView != null) {
            statusTextView.setSingleLine(false);
            statusTextView.setMaxLines(50);
            statusTextView.setTextSize(11);
            statusTextView.setText("Status: INITIALIZING L3 TARGET LOCK...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: L3 ARMED\n\nTURN DRONE ON.\nTAP 'CENTER ALL MOTORS' TO PULL FIRING CODES.");
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
            centerButton.setOnClickListener(v -> executeLevelThreeScan());
        }
    }

    private void executeLevelThreeScan() {
        try {
            StringBuilder report = new StringBuilder("=== L3 FIRING CODES ===\n");

            // 1. Find the hook to grab BaseProduct
            report.append("[1] CONNECTION HOOKS:\n");
            Method[] allMethods = Autel.class.getMethods(); // getMethods() gets inherited ones too
            for (Method m : allMethods) {
                if (m.getReturnType().getSimpleName().contains("Product") || m.getReturnType().getSimpleName().contains("Device")) {
                    report.append("  - ").append(m.getName()).append("() -> ").append(m.getReturnType().getSimpleName()).append("\n");
                }
            }

            // 2. Crack open Gimbal and extract motor commands
            report.append("\n[2] GIMBAL COMMANDS:\n");
            Class<?> baseProductClass = Class.forName("com.autel.sdk.product.BaseProduct");
            Method getGimbalMethod = null;
            for (Method m : baseProductClass.getDeclaredMethods()) {
                if (m.getName().equals("getGimbal")) {
                    getGimbalMethod = m;
                    break;
                }
            }

            if (getGimbalMethod != null) {
                Class<?> gimbalClass = getGimbalMethod.getReturnType();
                report.append("  Class: ").append(gimbalClass.getSimpleName()).append("\n");
                for (Method m : gimbalClass.getMethods()) {
                    String name = m.getName().toLowerCase();
                    // Hunt for any method that moves or sets the gimbal
                    if (name.contains("set") || name.contains("move") || name.contains("angle") || name.contains("rotation") || name.contains("control")) {
                        report.append("  - ").append(m.getName()).append("(");
                        Class<?>[] params = m.getParameterTypes();
                        for (int i=0; i<params.length; i++) {
                            report.append(params[i].getSimpleName());
                            if (i < params.length - 1) report.append(", ");
                        }
                        report.append(")\n");
                    }
                }
            }

            report.append("===========================\n");

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
