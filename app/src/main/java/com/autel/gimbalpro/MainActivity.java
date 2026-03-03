package com.autel.gimbalpro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
// import com.autel.sdk.GimbalManager; // Temporarily disabled to allow build

public class MainActivity extends AppCompatActivity {
    // private GimbalManager gimbalManager; // Temporarily disabled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // This is a placeholder to verify the build works!
        setTitle("Autel Gimbal Pro - Loaded");
    }
}
