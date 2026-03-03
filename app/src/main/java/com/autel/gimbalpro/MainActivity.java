package com.autel.gimbalpro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import com.autel.sdk.Autel;
import com.autel.sdk.product.BaseProduct;
import com.autel.sdk.ProductConnectListener;
import com.autel.common.CallbackWithNoParam;
import com.autel.common.error.AutelError;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "GimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setTitle("Initializing SDK...");

        Autel.init(this, "", new CallbackWithNoParam() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "SDK Initialized Successfully");
                runOnUiThread(() -> setTitle("Waiting for Gimbal..."));
            }

            @Override
            public void onFailure(AutelError error) {
                Log.e(TAG, "SDK Init Failed: " + error.getDescription());
                runOnUiThread(() -> setTitle("Init Failed: " + error.getDescription()));
            }
        });

        Autel.setProductConnectListener(new ProductConnectListener() {
            @Override
            public void productConnected(BaseProduct product) {
                runOnUiThread(() -> setTitle("Connected: " + product.getType()));
            }

            @Override
            public void productDisconnected() {
                runOnUiThread(() -> setTitle("Gimbal Disconnected"));
            }
        });
    }
}
