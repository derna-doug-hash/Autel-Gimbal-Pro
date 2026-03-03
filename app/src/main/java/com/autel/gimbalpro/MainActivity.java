package com.autel.gimbalpro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import com.autel.sdk.Autel;
import com.autel.sdk.product.BaseProduct;
import com.autel.sdk.ProductConnectListener;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "GimbalPro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // We use the standard layout we already have
        setContentView(R.layout.activity_main);
        
        setTitle("Waiting for Gimbal...");

        // Initialize the Autel engine
        Autel.init(this, false);

        // Set up the listener for the gimbal
        Autel.setProductConnectListener(new ProductConnectListener() {
            @Override
            public void productConnected(BaseProduct product) {
                Log.d(TAG, "Product Connected: " + product.getType());
                runOnUiThread(() -> setTitle("Gimbal Connected: " + product.getType()));
            }

            @Override
            public void productDisconnected() {
                Log.d(TAG, "Product Disconnected");
                runOnUiThread(() -> setTitle("Gimbal Disconnected"));
            }
        });
    }
}
