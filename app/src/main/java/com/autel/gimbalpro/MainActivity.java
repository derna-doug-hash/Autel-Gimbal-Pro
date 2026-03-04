package com.autel.gimbalpro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.ScrollView;

import com.autel.sdk.Autel;
import com.autel.sdk.product.BaseProduct;
import com.autel.sdk.ProductConnectListener;
import com.autel.common.CallbackWithNoParam;
import com.autel.common.error.AutelError;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Build a blank text screen to show the error
        ScrollView scroll = new ScrollView(this);
        errorText = new TextView(this);
        errorText.setTextSize(14f);
        errorText.setPadding(40, 40, 40, 40);
        scroll.addView(errorText);
        setContentView(scroll);

        // Intercept fatal crashes and print them to the screen instead of closing
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            
            runOnUiThread(() -> {
                errorText.setText("FATAL CRASH PREVENTED:\n\n" + sw.toString());
            });
            
            // Keep the app frozen open for 60 seconds so you can read/screenshot it
            try { Thread.sleep(60000); } catch (Exception e) {}
        });

        errorText.setText("Initializing SDK...");

        try {
            // Changed to getApplicationContext() to prevent ClassCastExceptions
            Autel.init(getApplicationContext(), "97ffacc0-c832-4a0d-b008-6f8e9e8cbd37", new CallbackWithNoParam() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> errorText.setText("Waiting for Gimbal..."));
                }

                @Override
                public void onFailure(AutelError error) {
                    runOnUiThread(() -> errorText.setText("Init Failed: " + error.getDescription()));
                }
            });

            Autel.setProductConnectListener(new ProductConnectListener() {
                @Override
                public void productConnected(BaseProduct product) {
                    runOnUiThread(() -> errorText.setText("Connected: " + product.getType()));
                }

                @Override
                public void productDisconnected() {
                    runOnUiThread(() -> errorText.setText("Gimbal Disconnected"));
                }
            });
            
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            errorText.setText("CAUGHT ERROR:\n\n" + sw.toString());
        }
    }
}
