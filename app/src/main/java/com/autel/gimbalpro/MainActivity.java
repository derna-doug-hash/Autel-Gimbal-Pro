package com.autel.gimbalpro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.autel.sdk.Autel;
import com.autel.sdk.product.BaseProduct;
import com.autel.sdk.ProductConnectListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // This wakes up the Autel SDK
        Autel.init(this, false);

        // This waits for you to plug in the gimbal
        Autel.setProductConnectListener(new ProductConnectListener() {
            @Override
            public void productConnected(BaseProduct product) {
                // When it connects, the top bar will change
                runOnUiThread(() -> setTitle("Gimbal Connected!"));
            }

            @Override
            public void productDisconnected() {
                runOnUiThread(() -> setTitle("Gimbal Disconnected"));
            }
        });
    }
}
