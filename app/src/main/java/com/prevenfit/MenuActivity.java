package com.prevenfit;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MenuActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_USB_TREE = 1001;
    private GridLayout bodyZoneGrid;
    private Uri usbUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        bodyZoneGrid = findViewById(R.id.bodyZoneGrid);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            getSharedPreferences("PrevenFitPrefs", MODE_PRIVATE).edit().remove("user_id").apply();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        findViewById(R.id.btnBackMain).setOnClickListener(v -> {
            getSharedPreferences("PrevenFitPrefs", MODE_PRIVATE).edit().remove("user_id").apply();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        // Try to load saved USB uri if previously granted
        SharedPreferences prefs = getSharedPreferences("USB_PREF", MODE_PRIVATE);
        String savedUri = prefs.getString("usb_uri", null);
        if (savedUri != null) {
            usbUri = Uri.parse(savedUri);
            // Check if the persisted URI permission is still valid
            try {
                getContentResolver().takePersistableUriPermission(usbUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                loadBodyZonesFromJson();
            } catch (SecurityException e) {
                // Permission might have been revoked, request again
                Toast.makeText(this, "USB access revoked, please re-select folder.", Toast.LENGTH_LONG).show();
                requestUsbFolderAccess();
            }
        } else {
            requestUsbFolderAccess();
        }
    }

    private void requestUsbFolderAccess() {
        Toast.makeText(this, "Please select your USB folder (root of USB)", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_USB_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_USB_TREE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                usbUri = data.getData();
                // Persist permission
                final int flags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(usbUri, flags);
                // Save uri for future use
                getSharedPreferences("USB_PREF", MODE_PRIVATE).edit()
                        .putString("usb_uri", usbUri.toString()).apply();
                loadBodyZonesFromJson();
            }
        }
    }

    private void loadBodyZonesFromJson() {
        try {
            Uri menuJsonUri = findMenuJsonFile(usbUri);
            if (menuJsonUri == null) {
                Toast.makeText(this, "menu.json not found in config folder on USB", Toast.LENGTH_LONG).show();
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(menuJsonUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            JSONObject json = new JSONObject(jsonBuilder.toString());
            JSONArray zones = json.getJSONArray("zones");
            bodyZoneGrid.removeAllViews(); // Clear existing buttons before adding new ones
            for (int i = 0; i < zones.length(); i++) {
                JSONObject zoneObj = zones.getJSONObject(i);
                String name = zoneObj.getString("name");
                String video = zoneObj.getString("video");
                JSONArray modesArray = zoneObj.getJSONArray("modes");
                String[] modes = new String[modesArray.length()];
                for (int j = 0; j < modesArray.length(); j++) {
                    // Assuming modes are simple strings, not objects with "name"
                    // If modes are objects like {"name": "Mode A"}, use modesArray.getJSONObject(j).getString("name")
                    modes[j] = modesArray.getString(j); // Changed to getString directly
                }
                addZoneButton(name, video, modes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading menu.json: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Uri findMenuJsonFile(Uri usbRootUri) {
        try {
            DocumentFile rootDoc = DocumentFile.fromTreeUri(this, usbRootUri);
            if (rootDoc == null || !rootDoc.isDirectory()) return null;

            DocumentFile configDir = rootDoc.findFile("config");
            if (configDir != null && configDir.isDirectory()) {
                DocumentFile menuJsonFile = configDir.findFile("menu.json");
                if (menuJsonFile != null && menuJsonFile.isFile()) {
                    return menuJsonFile.getUri();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addZoneButton(String zoneName, String video, String[] modes) {
        MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(zoneName);
        button.setTextColor(0xFF212121);
        button.setCornerRadius(16);
        button.setBackgroundTintList(getColorStateList(android.R.color.white));
        button.setStrokeWidth(1);
        button.setStrokeColorResource(android.R.color.darker_gray);
        button.setTextSize(16);
        button.setIconTintResource(android.R.color.holo_blue_dark);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_TOP);
        button.setRippleColorResource(android.R.color.holo_blue_light);
        button.setPadding(0, 50, 0, 50);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(12, 12, 12, 12);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ExerciseActivity.class);
            intent.putExtra("zone", zoneName);
            intent.putExtra("video", video);
            intent.putExtra("modes", modes);
            // Pass the USB URI to ExerciseActivity
            intent.putExtra("usb_uri", usbUri.toString());
            startActivity(intent);
        });
        bodyZoneGrid.addView(button);
    }
}
