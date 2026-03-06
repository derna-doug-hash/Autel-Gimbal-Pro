package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.sdk.product.BaseProduct;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;
import com.autel.sdk.product.AutelProductConnectListener;

// This import connects the code to your app's internal resources
import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Prepare the Configuration
        // The App Key is read automatically from your Manifest
        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        // 2. Initialize the SDK (The Handshake)
        // This stops the NullPointerException crash by waking up the SDK correctly
        Autel.init(this, config, new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Autel SDK Handshake Success");
                setupConnectionListener();
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "Autel SDK Handshake Failed: " + error.getDescription());
            }
        });
    }

    private void setupConnectionListener() {
        // 3. Listen for the Drone (V3 Method)
        Autel.setProductConnectListener(new AutelProductConnectListener() {
            @Override
            public void productConnected(BaseProduct product) {
                Log.d(TAG, "Drone Connected: " + product.getType());
                // Your gimbal logic will go here once we are inside
            }

            @Override
            public void productDisconnected() {
                Log.d(TAG, "Drone Disconnected");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up the SDK when the app closes
        Autel.destroy();
    }
}
