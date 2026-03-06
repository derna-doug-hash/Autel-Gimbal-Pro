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

import java.lang.reflect.Method;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";
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
            statusTextView.setText("Status: L-8.1 LOGCAT SCRAPER ARMED.\n\nOPEN YOUR LOG APP.\nFLIP SWITCH TO DUMP INTEL TO LOGS.");
            statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
        }

        if (reverseLogicSwitch != null) {
            reverseLogicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) executeL8DeepScrape();
            });
        }
    }

    private void executeL8DeepScrape() {
        // Force the UI update immediately on the main thread
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText("Status: SCRAPING TO LOGS. CHECK YOUR LOG APP FOR 'AutelGimbalPro'...");
                statusTextView.setTextColor(android.graphics.Color.RED);
            }
        });

        Log.i(TAG, "=== L-8.1 DEEP SCRAPER INITIATED ===");

        new Thread(() -> {
            String[] packages = {
                "com.autel.internal.gimbal.", 
                "com.autel.internal.sdk.gimbal.", 
                "com.autel.sdk.gimbal.",
                "com.autel.internal.video.",
                "com.autel.internal.remotecontroller."
            };
            
            String[] classNames = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "GimbalManager", "GimbalController", "AutelGimbalController"};

            for (String pkg : packages) {
                for (String cls : classNames) {
                    try {
                        Class<?> targetClass = Class.forName(pkg + cls);
                        Method[] methods = targetClass.getDeclaredMethods();

                        for (Method m : methods) {
                            Class<?>[] params = m.getParameterTypes();
                            // Looking for 2 to 4 primitive parameters (pitch, roll, yaw, etc.)
                            if (params.length >= 2 && params.length <= 4) {
                                boolean allNumeric = true;
                                for (Class<?> p : params) {
                                    String pName = p.getSimpleName().toLowerCase();
                                    if (!pName.equals("float") && !pName.equals("int") && !pName.equals("double") && !pName.equals("short") && !pName.equals("long")) {
                                        allNumeric = false;
                                        break;
                                    }
                                }
                                
                                if (allNumeric) {
                                    StringBuilder hit = new StringBuilder();
                                    hit.append("HIT: [").append(pkg).append(cls).append("] -> ").append(m.getName()).append("(");
                                    for (int i = 0; i < params.length; i++) {
                                        hit.append(params[i].getSimpleName());
                                        if (i < params.length - 1) hit.append(", ");
                                    }
                                    hit.append(")");
                                    Log.w(TAG, hit.toString()); // Using warning level (yellow) so it stands out in the logs
                                }
                            }
                        }
                    } catch (Throwable e) {
                        // Class doesn't exist, quietly continue
                    }
                }
            }
            Log.i(TAG, "=== L-8.1 SWEEP COMPLETE ===");
            
            runOnUiThread(() -> {
                if (statusTextView != null) {
                    statusTextView.setText("Status: SWEEP COMPLETE. CHECK LOGS.");
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
