package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;

// Import the internal resource file
import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the SDK Handshake for V3 hardware
        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        // Perform the Handshake (Authorization)
        // This is the CRITICAL fix to stop the NullPointerException crash
        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Autel SDK Handshake Success - Security Check Passed");
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Autel SDK Handshake Failed: " + error.getDescription());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up the SDK
        Autel.destroy();
    }
}
