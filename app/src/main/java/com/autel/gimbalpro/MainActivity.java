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
            statusTextView.setTextSize(11); // Even smaller to fit the payload
            statusTextView.setText("Status: INITIALIZING LEVEL 2 SCANNER...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: LEVEL 2 SCANNER ARMED\n\nTURN DRONE ON.\nTAP 'CENTER ALL MOTORS' TO EXECUTE.");
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
            centerButton.setOnClickListener(v -> executeLevelTwoScan());
        }
    }

    private void executeLevelTwoScan() {
        try {
            StringBuilder report = new StringBuilder("=== LEVEL 2 VAULT SCAN ===\n");
            
            // The likely hiding spots for the drone connection
            String[] targetClasses = {
                "com.autel.sdk.DeviceManager",
                "com.autel.sdk.ProductManager",
                "com.autel.sdk.AutelManager",
                "com.autel.sdk.product.BaseProduct"
            };

            for (String className : targetClasses) {
                try {
                    Class<?> clazz = Class.forName(className);
                    report.append("\n[+] FOUND VAULT: ").append(className.substring(className.lastIndexOf('.') + 1)).append("\n");
                    Method[] methods = clazz.getDeclaredMethods();
                    
                    int count = 0;
                    for (Method m : methods) {
                        String name = m.getName();
                        String ret = m.getReturnType().getSimpleName();
                        
                        // Filter out boring default Java methods
                        if (!name.equals("access$super") && !name.contains("lambda")) {
                            report.append("  - ").append(name).append("() -> ").append(ret).append("\n");
                            count++;
                        }
                    }
                    if (count == 0) report.append("  (Vault is empty)\n");
                    
                } catch (ClassNotFoundException e) {
                    report.append("[-] Missing Vault: ").append(className.substring(className.lastIndexOf('.') + 1)).append("\n");
                }
            }
            
            report.append("\n===========================\n");
            
            if (statusTextView != null) {
                statusTextView.setText(report.toString());
                statusTextView.setTextColor(android.graphics.Color.parseColor("#000000")); // Black text for readability
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
