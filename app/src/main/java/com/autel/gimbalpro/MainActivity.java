package com.autel.gimbalpro;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.autel.sdk.Autel;
import com.autel.sdk.product.BaseProduct;
import com.autel.common.error.AutelError;
import com.autel.sdk.SDKManager;
// We use the internal namespace for the resources (R)
import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutelGimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Autel SDK immediately to prevent the crash
        // This is the 'Handshake' that validates your App Key
        SDKManager.getManager().init(this, new com.autel.common.CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "SDK Init Success");
                checkConnection();
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "SDK Init Failed: " + error.getDescription());
            }
        });
    }

    private void checkConnection() {
        BaseProduct product = Autel.getDevice();
        if (product != null) {
            Log.d(TAG, "Drone Connected: " + product.getType());
        } else {
            Log.d(TAG, "No Drone Detected");
        }
    }
}
