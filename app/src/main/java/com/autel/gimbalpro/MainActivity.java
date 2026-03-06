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

        // Prep the screen for a massive data dump
        if (statusTextView != null) {
            statusTextView.setSingleLine(false);
            statusTextView.setMaxLines(50);
            statusTextView.setTextSize(12); // Make it small to fit
            statusTextView.setText("Status: INITIALIZING DEEP SCANNER...");
        }

        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: SCANNER ARMED\n\nTURN DRONE ON.\nONCE CONNECTED, TAP 'CENTER ALL MOTORS' TO EXECUTE SCAN.");
                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                    }
                });
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Handshake Failed: " + error.getDescription());
            }
        });

        // Use the Center button as the trigger for the scan
        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeDeepScan());
        }
    }

    private void executeDeepScan() {
        try {
            StringBuilder report = new StringBuilder("=== AUTEL V3 SDK BRAIN DUMP ===\n");
            
            // Scan the main Autel class to find the exact hidden methods
            Method[] methods = Autel.class.getDeclaredMethods();
            for (Method m : methods) {
                String name = m.getName();
                String ret = m.getReturnType().getSimpleName();
                
                // We only want methods that return an object (like a drone or product)
                if (!ret.equals("void") && !ret.equals("boolean") && !ret.equals("int")) {
                    report.append("Found: Autel.").append(name).append("() -> Returns: ").append(ret).append("\n");
                }
            }
            
            report.append("===========================\n");
            
            if (statusTextView != null) {
                statusTextView.setText(report.toString());
            }
        } catch (Exception e) {
            if (statusTextView != null) {
                statusTextView.setText("Scan Error: " + e.toString());
            }
        }
    }

    // Dynamic UI Finder
    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            
            if (child instanceof TextView && !(child instanceof Button) && !(child instanceof Switch)) {
                String text = ((TextView) child).getText().toString().toUpperCase();
                if (text.contains("STATUS") || text.contains("UNKNOWN")) {
                    statusTextView = (TextView) child;
                }
            } else if (child instanceof Button) {
                String text = ((Button) child).getText().toString().toUpperCase();
                if (text.contains("CENTER")) {
                    centerButton = (Button) child;
                }
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
