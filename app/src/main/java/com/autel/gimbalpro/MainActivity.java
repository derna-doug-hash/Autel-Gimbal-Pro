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
    private TextView statusTextView;
    private Button centerButton;
    private Switch reverseLogicSwitch;
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
            statusTextView.setText("Status: INITIALIZING L7.1 ARMORED CAPTURE...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder().setPostOnUi(true).create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                try {
                    Autel.setProductConnectListener(new ProductConnectListener() {
                        @Override
                        public void productConnected(BaseProduct product) {
                            try {
                                liveDrone = product;
                                final String name = (product != null) ? product.getClass().getName() : "NULL PRODUCT";
                                runOnUiThread(() -> {
                                    if (statusTextView != null) {
                                        statusTextView.setText("Status: HVT CAPTURED!\nClass: " + name + "\n\nTAP 'CENTER ALL MOTORS' TO INTERROGATE.");
                                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                                    }
                                });
                            } catch (Exception e) {
                                logError("Capture Error: " + e.getMessage());
                            }
                        }

                        @Override
                        public void productDisconnected() {
                            liveDrone = null;
                            logError("HVT LOST (DISCONNECTED).");
                        }
                    });
                    
                    runOnUiThread(() -> {
                        if (statusTextView != null && liveDrone == null) {
                            statusTextView.setText("Status: LISTENING POST ACTIVE.\n\nWAITING FOR DRONE TO LINK...");
                        }
                    });
                } catch (Exception e) {
                    logError("Listener Error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(AutelError error) {
                logError("Handshake Failed: " + error.getDescription());
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeInterrogation());
        }

        // The Secondary Offline Breach (Activated by the Switch)
        if (reverseLogicSwitch != null) {
            reverseLogicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) executeBlindInternalSweep();
            });
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

    private void executeInterrogation() {
        if (liveDrone == null) {
            logError("NO HVT IN CUSTODY. (If stuck, flip Reverse Roll switch for Offline Breach)");
            return;
        }

        try {
            StringBuilder report = new StringBuilder("=== L7.1 INTERROGATION ===\n");
            
            Method getGimbalMethod = liveDrone.getClass().getMethod("getGimbal");
            Object liveGimbal = getGimbalMethod.invoke(liveDrone);
            
            if (liveGimbal != null) {
                Class<?> realGimbalClass = liveGimbal.getClass();
                report.append("REAL GIMBAL CLASS: ").append(realGimbalClass.getName()).append("\n\n");
                
                for (Method m : realGimbalClass.getDeclaredMethods()) {
                    String name = m.getName();
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
            logError("Interrogation Error: " + e.getMessage());
        }
    }

    // The SIGINT Pipeline Breach
    private void executeBlindInternalSweep() {
        try {
            StringBuilder report = new StringBuilder("=== SIGINT OFFLINE BREACH ===\n");
            String[] targets = {"a", "b", "c", "d", "e", "GimbalManager", "RxAutelGimbalInternal"};
            boolean foundAny = false;

            for (String t : targets) {
                try {
                    Class<?> clazz = Class.forName("com.autel.internal.gimbal." + t);
                    report.append("\n[+] Found Obfuscated File: ").append(t).append(".class\n");
                    foundAny = true;
                    for (Method m : clazz.getDeclaredMethods()) {
                        String name = m.getName().toLowerCase();
                        if(name.contains("set") || name.contains("angle") || name.contains("move") || name.contains("rotation")) {
                            report.append("  - ").append(m.getName()).append("\n");
                        }
                    }
                } catch (Exception e) {
                    // Ignore missing files
                }
            }

            if (!foundAny) report.append("No internal gimbal files found. Deeper obfuscation present.");

            if (statusTextView != null) {
                statusTextView.setText(report.toString());
                statusTextView.setTextColor(android.graphics.Color.parseColor("#000000"));
            }
        } catch (Exception e) {
            logError("Sweep Error: " + e.toString());
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
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}package com.autel.gimbalpro;

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
    private TextView statusTextView;
    private Button centerButton;
    private Switch reverseLogicSwitch;
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
            statusTextView.setText("Status: INITIALIZING L7.1 ARMORED CAPTURE...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder().setPostOnUi(true).create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                try {
                    Autel.setProductConnectListener(new ProductConnectListener() {
                        @Override
                        public void productConnected(BaseProduct product) {
                            try {
                                liveDrone = product;
                                final String name = (product != null) ? product.getClass().getName() : "NULL PRODUCT";
                                runOnUiThread(() -> {
                                    if (statusTextView != null) {
                                        statusTextView.setText("Status: HVT CAPTURED!\nClass: " + name + "\n\nTAP 'CENTER ALL MOTORS' TO INTERROGATE.");
                                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                                    }
                                });
                            } catch (Exception e) {
                                logError("Capture Error: " + e.getMessage());
                            }
                        }

                        @Override
                        public void productDisconnected() {
                            liveDrone = null;
                            logError("HVT LOST (DISCONNECTED).");
                        }
                    });
                    
                    runOnUiThread(() -> {
                        if (statusTextView != null && liveDrone == null) {
                            statusTextView.setText("Status: LISTENING POST ACTIVE.\n\nWAITING FOR DRONE TO LINK...");
                        }
                    });
                } catch (Exception e) {
                    logError("Listener Error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(AutelError error) {
                logError("Handshake Failed: " + error.getDescription());
            }
        });

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeInterrogation());
        }

        // The Secondary Offline Breach (Activated by the Switch)
        if (reverseLogicSwitch != null) {
            reverseLogicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) executeBlindInternalSweep();
            });
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

    private void executeInterrogation() {
        if (liveDrone == null) {
            logError("NO HVT IN CUSTODY. (If stuck, flip Reverse Roll switch for Offline Breach)");
            return;
        }

        try {
            StringBuilder report = new StringBuilder("=== L7.1 INTERROGATION ===\n");
            
            Method getGimbalMethod = liveDrone.getClass().getMethod("getGimbal");
            Object liveGimbal = getGimbalMethod.invoke(liveDrone);
            
            if (liveGimbal != null) {
                Class<?> realGimbalClass = liveGimbal.getClass();
                report.append("REAL GIMBAL CLASS: ").append(realGimbalClass.getName()).append("\n\n");
                
                for (Method m : realGimbalClass.getDeclaredMethods()) {
                    String name = m.getName();
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
            logError("Interrogation Error: " + e.getMessage());
        }
    }

    // The SIGINT Pipeline Breach
    private void executeBlindInternalSweep() {
        try {
            StringBuilder report = new StringBuilder("=== SIGINT OFFLINE BREACH ===\n");
            String[] targets = {"a", "b", "c", "d", "e", "GimbalManager", "RxAutelGimbalInternal"};
            boolean foundAny = false;

            for (String t : targets) {
                try {
                    Class<?> clazz = Class.forName("com.autel.internal.gimbal." + t);
                    report.append("\n[+] Found Obfuscated File: ").append(t).append(".class\n");
                    foundAny = true;
                    for (Method m : clazz.getDeclaredMethods()) {
                        String name = m.getName().toLowerCase();
                        if(name.contains("set") || name.contains("angle") || name.contains("move") || name.contains("rotation")) {
                            report.append("  - ").append(m.getName()).append("\n");
                        }
                    }
                } catch (Exception e) {
                    // Ignore missing files
                }
            }

            if (!foundAny) report.append("No internal gimbal files found. Deeper obfuscation present.");

            if (statusTextView != null) {
                statusTextView.setText(report.toString());
                statusTextView.setTextColor(android.graphics.Color.parseColor("#000000"));
            }
        } catch (Exception e) {
            logError("Sweep Error: " + e.toString());
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
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
