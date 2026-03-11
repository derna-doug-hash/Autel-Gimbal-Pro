package com.autel.gimbalpro;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private LinearLayout buttonContainer;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Creating the UI programmatically for the dynamic list
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        
        statusTextView = new TextView(this);
        statusTextView.setText("Status: L-18 INTENT SCRAPER ARMED.\nScanning Autel Packages for hidden engineering screens...");
        statusTextView.setPadding(20, 20, 20, 20);
        statusTextView.setTextColor(android.graphics.Color.BLUE);
        root.addView(statusTextView);

        ScrollView scrollView = new ScrollView(this);
        buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(buttonContainer);
        
        root.addView(scrollView);
        setContentView(root);

        scanForHiddenActivities();
    }

    private void scanForHiddenActivities() {
        PackageManager pm = getPackageManager();
        // Scanning both the standard and enterprise versions of the Autel App
        String[] targetPackages = {"com.autelrobotics.enterprise", "com.autelrobotics.explorer"};
        int count = 0;
        
        for (String targetPackage : targetPackages) {
            try {
                PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_ACTIVITIES);
                if (info.activities != null) {
                    for (ActivityInfo activity : info.activities) {
                        String name = activity.name.toLowerCase();
                        // Filter for interesting sounding hidden menus
                        if (name.contains("setting") || name.contains("gimbal") || 
                            name.contains("engineer") || name.contains("factory") || 
                            name.contains("debug") || name.contains("calib") || 
                            name.contains("test")) {
                            
                            addButton(activity.name, targetPackage);
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                // Package not found on this device, continue to the next one
            }
        }
        
        if (count > 0) {
            statusTextView.setText("SCAN COMPLETE: Found " + count + " High-Value Targets.\nTap a button to breach that hidden screen.");
            statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
        } else {
            statusTextView.setText("NEGATIVE CONTACT. No hidden activities found in target packages.");
            statusTextView.setTextColor(android.graphics.Color.RED);
        }
    }

    private void addButton(final String activityName, final String pkgName) {
        Button btn = new Button(this);
        // Clean up the name for the button text so it's readable
        String[] parts = activityName.split("\\.");
        String shortName = parts[parts.length - 1];
        
        btn.setText(shortName);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setClassName(pkgName, activityName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                statusTextView.setText("BREACH INITIATED: " + shortName);
                statusTextView.setTextColor(android.graphics.Color.BLUE);
            } catch (Exception e) {
                statusTextView.setText("BREACH FAILED for " + shortName + ": \nSecurity locked.");
                statusTextView.setTextColor(android.graphics.Color.RED);
            }
        });
        buttonContainer.addView(btn);
    }
}
