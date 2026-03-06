package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;
import com.autel.sdk.AutelSdkConfig;
import com.autel.sdk.product.BaseProduct;
import com.autel.common.error.AutelError;
import com.autel.common.CallbackWithNoParam;
import com.autel.sdk.product.ProductConnectListener; // Renamed to match your SDK
import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the SDK for the V3 series
        AutelSdkConfig config = new AutelSdkConfig.AutelSdkConfigBuilder()
                .setPostOnUi(true)
                .create();

        // Perform the Handshake (Authorization)
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
        // Correct Listener for your autel-sdk-release.aar version
        Autel.setProductConnectListener(new ProductConnectListener() {
            @Override
            public void productConnected(BaseProduct product) {
                Log.d(TAG, "Drone Connected: " + product.getType());
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
        Autel.destroy();
    }
}
