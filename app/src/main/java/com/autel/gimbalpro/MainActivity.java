package com.autel.gimbalpro;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private TextView statusTextView;
    private Button centerButton;
    private Switch reverseLogicSwitch;

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
            statusTextView.setText("Status: L-8 DEEP SCRAPER ARMED.\n\nNO DRONE CONNECTION REQUIRED.\nFLIP 'REVERSE ROLL LOGIC' TO EXECUTE AREA SWEEP.");
            statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
        }

        if (reverseLogicSwitch != null) {
            reverseLogicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) executeL8DeepScrape();
            });
        }
    }

    private void executeL8DeepScrape() {
        if (statusTextView != null) {
            statusTextView.setText("Status: EXECUTING BRUTE FORCE SWEEP. STANDBY...");
            statusTextView.setTextColor(android.graphics.Color.RED);
        }

        new Thread(() -> {
            StringBuilder report = new StringBuilder("=== L-8 AREA BDA ===\n");
            
            // The drop zones
            String[] packages = {
                "com.autel.internal.gimbal.", 
                "com.autel.internal.sdk.gimbal.", 
                "com.autel.sdk.gimbal.",
                "com.autel.internal.video.",
                "com.autel.internal.remotecontroller."
            };
            
            // The obfuscated target list
            String[] classNames = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "GimbalManager", "GimbalController", "AutelGimbalController"};

            boolean foundSomething = false;

            for (String pkg : packages) {
                for (String cls : classNames) {
                    try {
                        Class<?> targetClass = Class.forName(pkg + cls);
                        Method[] methods = targetClass.getDeclaredMethods();

                        for (Method m : methods) {
                            Class<?>[] params = m.getParameterTypes();
                            // Look for methods taking 2, 3, or 4 parameters (Pitch, Roll, Yaw, Time/Speed)
                            if (params.length >= 2 && params.length <= 4) {
                                boolean allNumeric = true;
                                for (Class<?> p : params) {
                                    String pName = p.getSimpleName().toLowerCase();
                                    if (!pName.equals("float") && !pName.equals("int") && !pName.equals("double") && !pName.equals("short") && !pName.equals("long")) {
                                        allNumeric = false;
                                        break;
                                    }
                                }
                                
                                // Only log it if it matches our numeric firing signature
                                if (allNumeric) {
                                    if (!foundSomething) {
                                        report.append("\n[!] HIGH PROBABILITY TARGETS ACQUIRED:\n");
                                        foundSomething = true;
                                    }
                                    report.append("Class [").append(cls).append("] -> ").append(m.getName()).append("(");
                                    for (int i = 0; i < params.length; i++) {
                                        report.append(params[i].getSimpleName());
                                        if (i < params.length - 1) report.append(", ");
                                    }
                                    report.append(")\n");
                                }
                            }
                        }
                    } catch (Throwable e) {
                        // Class doesn't exist or is locked, keep moving
                    }
                }
            }

            if (!foundSomething) report.append("\nNo numeric firing sequences found in grid.\n");

            runOnUiThread(() -> {
                if (statusTextView != null) {
                    statusTextView.setText(report.toString());
                    statusTextView.setTextColor(android.graphics.Color.parseColor("#000000"));
                }
            });
        }).start();
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
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
