package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.sdk.product.BaseProduct;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;

// We'll use the internal namespace for resources
import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the SDK for V3 hardware
        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        // Perform the Handshake (Authorization)
        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Autel SDK Handshake Success");
                // Instead of a listener that might have a different name,
                // we'll just check if the device is already there.
                checkInitialConnection();
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Autel SDK Handshake Failed: " + error.getDescription());
            }
        });
    }

    private void checkInitialConnection() {
        // Most stable way to check connection without specific listener imports
        BaseProduct product = Autel.getDevice();
        if (product != null) {
            Log.d(TAG, "Drone Detected: " + product.getType());
        } else {
            Log.d(TAG, "Waiting for Drone connection...");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up the SDK when the app closes
        Autel.destroy();
    }
}
