package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";
    private TextView statusTextView;
    private Button centerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Dynamically wire up the UI to prevent build failures
        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        // 2. Set initial state
        if (statusTextView != null) {
            statusTextView.setText("Status: INITIALIZING UI...");
        }

        // 3. Test the Button
        if (centerButton != null) {
            centerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: UI ACTIVE - MOTORS CENTERED (SIMULATED)");
                    }
                }
            });
        }

        // 4. Configure the SDK
        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        // 5. Perform the Handshake
        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Autel SDK Handshake Success");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (statusTextView != null) {
                            statusTextView.setText("Status: HANDSHAKE OK - WAITING ON GIMBAL API");
                            statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00")); // Green
                        }
                    }
                });
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Autel SDK Handshake Failed: " + error.getDescription());
            }
        });
    }

    // Recursive function to automatically find your XML elements without needing exact IDs
    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            
            if (child instanceof TextView && !(child instanceof Button)) {
                String text = ((TextView) child).getText().toString().toUpperCase();
                if (text.contains("STATUS") || text.contains("UNKNOWN")) {
                    statusTextView = (TextView) child;
                }
            }
            
            if (child instanceof Button) {
                String text = ((Button) child).getText().toString().toUpperCase();
                if (text.contains("CENTER")) {
                    centerButton = (Button) child;
                }
            }

            if (child instanceof ViewGroup) {
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
