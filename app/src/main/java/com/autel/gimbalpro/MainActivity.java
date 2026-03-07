package com.autel.gimbalpro;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private TextView statusTextView;
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
            statusTextView.setText("Status: L-8.2 DIRECT-TO-DISK ARMED.\n\nFLIP SWITCH TO WRITE INTEL TO DOWNLOADS FOLDER.");
            statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
        }

        if (reverseLogicSwitch != null) {
            reverseLogicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) executeL8DirectToDisk();
            });
        }
    }

    private void executeL8DirectToDisk() {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText("Status: WRITING TO DISK. DO NOT CLOSE APP...");
                statusTextView.setTextColor(android.graphics.Color.RED);
            }
        });

        new Thread(() -> {
            // Setup the Drop Zone (Downloads Folder)
            File dropZone = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File intelFile = new File(dropZone, "Autel_Intel_Drop.txt");
            
            boolean foundSomething = false;

            try (FileWriter writer = new FileWriter(intelFile, false)) { // 'false' overwrites old files
                writer.append("=== L-8.2 TACTICAL INTEL DROP ===\n\n");

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
                                        hit.append(")\n");
                                        writer.append(hit.toString());
                                        foundSomething = true;
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            // Class missing or locked, keep pushing
                        }
                    }
                }

                if (!foundSomething) {
                    writer.append("\nNEGATIVE CONTACT. No numeric firing sequences found in scanned sectors.\n");
                } else {
                    writer.append("\n=== SWEEP COMPLETE ===");
                }

                final String finalPath = intelFile.getAbsolutePath();
                
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: INTEL SECURED!\n\nFile saved to:\n" + finalPath + "\n\nExtract via File Manager.");
                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                    }
                });

            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("ERROR WRITING TO DISK: " + errorMsg + "\nCheck app storage permissions.");
                        statusTextView.setTextColor(android.graphics.Color.RED);
                    }
                });
            }
        }).start();
    }

    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button) && !(child instanceof Switch)) {
                String text = ((TextView) child).getText().toString().toUpperCase();
                if (text.contains("STATUS") || text.contains("UNKNOWN")) statusTextView = (TextView) child;
            } else if (child instanceof Switch) {
                reverseLogicSwitch = (Switch) child;
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
