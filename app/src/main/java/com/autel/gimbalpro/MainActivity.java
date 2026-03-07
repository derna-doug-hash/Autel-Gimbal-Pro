package com.autel.gimbalpro;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.autel.sdk.Autel;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.autel.gimbalpro.R;

public class MainActivity extends AppCompatActivity {
    private TextView statusTextView;
    private Button centerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewGroup root = findViewById(android.R.id.content);
        findUIElements(root);

        updateStatus("Status: L-15 SDK X-RAY ARMED.\n\nTAP 'CENTER' TO DUMP SDK BLUEPRINTS.", android.graphics.Color.BLUE);

        if (centerButton != null) {
            centerButton.setOnClickListener(v -> executeXRay());
        }
    }

    private void executeXRay() {
        updateStatus("SCANNING SDK... DO NOT CLOSE APP", android.graphics.Color.RED);
        
        new Thread(() -> {
            File dropZone = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File intelFile = new File(dropZone, "Autel_SDK_Dump.txt");
            
            try (FileWriter writer = new FileWriter(intelFile, false)) {
                writer.append("=== L-15 SDK X-RAY: Autel.class ===\n\n");
                
                writer.append("--- DECLARED FIELDS (HIDDEN VARIABLES) ---\n");
                for (Field f : Autel.class.getDeclaredFields()) {
                    writer.append(f.getType().getSimpleName()).append(" ")
                          .append(f.getName()).append("\n");
                }
                
                writer.append("\n--- DECLARED METHODS (HIDDEN FUNCTIONS) ---\n");
                for (Method m : Autel.class.getDeclaredMethods()) {
                    writer.append(m.getReturnType().getSimpleName()).append(" ")
                          .append(m.getName()).append("(");
                    
                    Class<?>[] params = m.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        writer.append(params[i].getSimpleName());
                        if (i < params.length - 1) writer.append(", ");
                    }
                    writer.append(")\n");
                }
                
                final String path = intelFile.getAbsolutePath();
                updateStatus("X-RAY COMPLETE!\nFile saved to:\n" + path + "\n\nExtract via File Manager.", android.graphics.Color.parseColor("#00AA00"));
                
            } catch (Exception e) {
                updateStatus("ERROR WRITING TO DISK: " + e.getMessage(), android.graphics.Color.RED);
            }
        }).start();
    }

    private void updateStatus(String msg, int color) {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText(msg);
                statusTextView.setTextColor(color);
            }
        });
    }

    private void findUIElements(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button)) {
                if (child.getId() != View.NO_ID) statusTextView = (TextView) child;
            } else if (child instanceof Button) {
                String text = ((Button) child).getText().toString().toUpperCase();
                if (text.contains("CENTER")) centerButton = (Button) child;
            } else if (child instanceof ViewGroup) {
                findUIElements((ViewGroup) child);
            }
        }
    }
}
