package com.autel.gimbalpro;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;

public class MainActivity extends AppCompatActivity {
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        statusTextView = new TextView(this);
        statusTextView.setText("Status: L-18.1 TARGET COORDINATOR ARMED.\nExtracting exact ADB strike coordinates...");
        statusTextView.setPadding(40, 40, 40, 40);
        statusTextView.setTextSize(14f);
        statusTextView.setTextColor(android.graphics.Color.BLUE);
        
        setContentView(statusTextView);

        extractAdbCommands();
    }

    private void extractAdbCommands() {
        new Thread(() -> {
            File dropZone = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File intelFile = new File(dropZone, "Autel_ADB_Strike_Coords.txt");
            
            PackageManager pm = getPackageManager();
            String[] targetPackages = {"com.autelrobotics.enterprise", "com.autelrobotics.explorer"};
            int count = 0;
            
            try (FileWriter writer = new FileWriter(intelFile, false)) {
                writer.append("=== L-18.1 ADB STRIKE COORDINATES ===\n\n");
                writer.append("To force-launch these hidden screens, connect the Smart Controller to a PC with ADB installed, open a command prompt, and paste the commands below:\n\n");

                for (String targetPackage : targetPackages) {
                    try {
                        PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_ACTIVITIES);
                        if (info.activities != null) {
                            for (ActivityInfo activity : info.activities) {
                                String name = activity.name.toLowerCase();
                                if (name.contains("setting") || name.contains("gimbal") || 
                                    name.contains("engineer") || name.contains("factory") || 
                                    name.contains("debug") || name.contains("calib") || 
                                    name.contains("test")) {
                                    
                                    // Craft the exact ADB command
                                    String adbCommand = "adb shell am start -n " + targetPackage + "/" + activity.name;
                                    writer.append("[ TARGET: ").append(activity.name).append(" ]\n");
                                    writer.append(adbCommand).append("\n\n");
                                    count++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Package missing, skip
                    }
                }
                
                final int finalCount = count;
                final String finalPath = intelFile.getAbsolutePath();
                
                runOnUiThread(() -> {
                    if (finalCount > 0) {
                        statusTextView.setText("INTEL SECURED!\nGenerated ADB commands for " + finalCount + " targets.\n\nFile saved to:\n" + finalPath + "\n\nExtract via File Manager to your PC.");
                        statusTextView.setTextColor(android.graphics.Color.parseColor("#00AA00"));
                    } else {
                        statusTextView.setText("NEGATIVE CONTACT. Extraction failed.");
                        statusTextView.setTextColor(android.graphics.Color.RED);
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusTextView.setText("ERROR WRITING TO DISK: " + e.getMessage());
                    statusTextView.setTextColor(android.graphics.Color.RED);
                });
            }
        }).start();
    }
}
