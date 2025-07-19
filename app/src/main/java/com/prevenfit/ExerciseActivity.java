package com.prevenfit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.UriPermission;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile; // Import DocumentFile
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader; // Import BufferedReader
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader; // Import InputStreamReader
import java.nio.charset.StandardCharsets; // Import StandardCharsets
import java.util.ArrayList;
import java.util.List;

public class ExerciseActivity extends AppCompatActivity {
    private TextView textVideoTitle, textUserId, textResistance, textTimer, textReps, textMode;
    private Button btnStart, btnIncrease, btnDecrease;
    // Removed folderPickerLauncher as USB URI is passed from MenuActivity
    private Spinner spinnerMode, spinnerZone; // Re-added spinnerZone
    private PlayerView playerView;
    private ExoPlayer player;
    private Handler handler;
    private Runnable timerRunnable;
    private Runnable inactivityRunnable;
    private int resistanceLevel = 12;
    private int secondsPassed = 0;
    private int repetitionCount = 0;
    private boolean isSessionRunning = false;
    private SharedPreferences prefs;
    private static final int INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000;
    private String currentUserId = "User";
    private JSONObject menuJsonObject; // This will now hold the entire menu.json content
    private String selectedVideoFile = "default.mp4";
    private Uri usbRootUri; // Changed from File usbRoot to Uri usbRootUri
    private String initialZoneName; // To store the zone name passed from MenuActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exercise_activity);

        // Bind views
        textVideoTitle = findViewById(R.id.textVideoTitle);
        textUserId = findViewById(R.id.textUserId);
        textResistance = findViewById(R.id.textResistance);
        textTimer = findViewById(R.id.textTimer);
        textReps = findViewById(R.id.textReps);
        textMode = findViewById(R.id.textMode);
        btnStart = findViewById(R.id.btnStart);
        btnIncrease = findViewById(R.id.btnIncrease);
        btnDecrease = findViewById(R.id.btnDecrease);
        spinnerMode = findViewById(R.id.spinnerMode);
        spinnerZone = findViewById(R.id.spinnerZone); // Re-initialized spinnerZone
        playerView = findViewById(R.id.videoView);

        prefs = getSharedPreferences("PrevenFitPrefs", Context.MODE_PRIVATE);
        handler = new Handler();

        currentUserId = prefs.getString("user_id", "User");
        textUserId.setText("User: " + currentUserId);

        // Get USB URI and initial zone/video/modes from Intent
        if (getIntent().hasExtra("usb_uri")) {
            usbRootUri = Uri.parse(getIntent().getStringExtra("usb_uri"));
            initialZoneName = getIntent().getStringExtra("zone");
            selectedVideoFile = getIntent().getStringExtra("video"); // Get initial video
            // Modes are handled by populateZones based on selected zone
        } else {
            Toast.makeText(this, "USB access URI not provided. Please restart from Menu.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if URI is missing
            return;
        }

        // Load menu.json using the passed USB URI
        loadMenuJson();
        if (menuJsonObject != null) {
            populateZones();
        } else {
            Toast.makeText(this, "Could not load menu.json. Please ensure it's in the USB config folder.", Toast.LENGTH_LONG).show();
            // Optionally, finish the activity if menu.json is critical
            // finish();
            // return;
        }

        btnIncrease.setOnClickListener(v -> {
            resistanceLevel++;
            textResistance.setText(String.valueOf(resistanceLevel));
        });
        btnDecrease.setOnClickListener(v -> {
            if (resistanceLevel > 1) resistanceLevel--;
            textResistance.setText(String.valueOf(resistanceLevel));
        });
        btnStart.setOnClickListener(v -> {
            if (!isSessionRunning) startSession();
        });

        // This permission check is for general file access, SAF handles specific URI permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }

        findViewById(R.id.btnBackMain).setOnClickListener(v -> finish());
        findViewById(R.id.btnHomeMain).setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
        findViewById(R.id.btnProgress).setOnClickListener(v -> startActivity(new Intent(this, ProgressActivity.class)));

        textResistance.setText(String.valueOf(resistanceLevel));
        startInactivityTimer();
    }

    // Removed detectUsbStorage() as USB URI is passed from MenuActivity

    private void loadMenuJson() {
        try {
            DocumentFile rootDoc = DocumentFile.fromTreeUri(this, usbRootUri);
            if (rootDoc == null || !rootDoc.isDirectory()) {
                Toast.makeText(this, "Invalid USB root URI.", Toast.LENGTH_LONG).show();
                menuJsonObject = null;
                return;
            }

            DocumentFile configDir = rootDoc.findFile("config");
            if (configDir == null || !configDir.isDirectory()) {
                Toast.makeText(this, "Config folder not found on USB.", Toast.LENGTH_LONG).show();
                menuJsonObject = null;
                return;
            }

            DocumentFile configFile = configDir.findFile("menu.json");
            if (configFile == null || !configFile.isFile()) {
                Toast.makeText(this, "menu.json not found in config folder on USB.", Toast.LENGTH_LONG).show();
                menuJsonObject = null;
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(configFile.getUri());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            String jsonStr = jsonBuilder.toString();
            menuJsonObject = new JSONObject(jsonStr);
            Toast.makeText(this, "menu.json loaded successfully.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error loading menu.json: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("USB_ERROR", "Exception loading menu.json: ", e);
            menuJsonObject = null;
        }
    }

    private void populateZones() {
        try {
            JSONArray zones = menuJsonObject.getJSONArray("zones");
            List<String> zoneNames = new ArrayList<>();
            int initialZonePosition = 0; // To pre-select the zone passed from MenuActivity

            for (int i = 0; i < zones.length(); i++) {
                String name = zones.getJSONObject(i).getString("name");
                zoneNames.add(name);
                if (name.equals(initialZoneName)) {
                    initialZonePosition = i;
                }
            }

            ArrayAdapter<String> zoneAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, zoneNames);
            zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerZone.setAdapter(zoneAdapter);
            spinnerZone.setSelection(initialZonePosition); // Set initial selection

            spinnerZone.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    try {
                        JSONObject selectedZone = zones.getJSONObject(position);
                        selectedVideoFile = selectedZone.getString("video");
                        textVideoTitle.setText(selectedZone.getString("name")); // Update video title with zone name

                        List<String> modeList = new ArrayList<>();
                        JSONArray modesArray = selectedZone.getJSONArray("modes");
                        for (int j = 0; j < modesArray.length(); j++) {
                            // Change this line:
                            // modeList.add(modesArray.getString(j));
                            // To this:
                            modeList.add(modesArray.getJSONObject(j).getString("name"));
                        }
                        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(ExerciseActivity.this,
                                android.R.layout.simple_spinner_item, modeList);
                        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerMode.setAdapter(modeAdapter);
                        spinnerMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                textMode.setText("Mode: " + parent.getItemAtPosition(position).toString());
                            }
                            @Override
                            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                        });
                        // Set initial mode text for the newly selected zone
                        if (!modeList.isEmpty()) {
                            textMode.setText("Mode: " + modeList.get(0));
                        } else {
                            textMode.setText("Mode: N/A");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ExerciseActivity.this, "Error parsing zone/mode data.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            // Manually trigger selection for the initial zone to populate modes and video
            // This ensures modes are loaded even if the user doesn't interact with the spinner
            spinnerZone.setSelection(initialZonePosition, true); // true to trigger listener

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error populating zones from menu.json. Ensure 'zones' array is present.", Toast.LENGTH_LONG).show();
        }
    }

    private void startSession() {
        isSessionRunning = true;
        secondsPassed = 0;
        repetitionCount = 0;
        btnIncrease.setEnabled(false);
        btnDecrease.setEnabled(false);
        btnStart.setEnabled(false);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Use DocumentFile to access the video file via URI
        Uri videoUri = null;
        try {
            DocumentFile rootDoc = DocumentFile.fromTreeUri(this, usbRootUri);
            if (rootDoc != null) {
                DocumentFile videosDir = rootDoc.findFile("videos");
                if (videosDir != null && videosDir.isDirectory()) {
                    DocumentFile videoFileDoc = videosDir.findFile(selectedVideoFile);
                    if (videoFileDoc != null && videoFileDoc.isFile()) {
                        videoUri = videoFileDoc.getUri();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("USB_VIDEO", "Error finding video file via SAF: " + e.getMessage());
        }

        if (videoUri == null) {
            Toast.makeText(this, "Video not found: " + selectedVideoFile, Toast.LENGTH_SHORT).show();
            stopSession(); // Stop session if video not found
            return;
        }

        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) stopSession();
            }
        });

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsPassed++;
                if (secondsPassed % 5 == 0) repetitionCount++;
                textTimer.setText(String.format("%02d:%02d", secondsPassed / 60, secondsPassed % 60));
                textReps.setText(String.valueOf(repetitionCount));
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(timerRunnable, 1000);
    }

    private void stopSession() {
        isSessionRunning = false;
        if (player != null) {
            player.stop();
            player.release(); // Release player resources
            player = null;
        }
        handler.removeCallbacks(timerRunnable);
        btnIncrease.setEnabled(true);
        btnDecrease.setEnabled(true);
        btnStart.setEnabled(true);
        saveLog();
        Toast.makeText(this, "Session Complete\nReps: " + repetitionCount +
                "\nTime: " + secondsPassed + " sec\nMode: " + textMode.getText(), Toast.LENGTH_LONG).show();
    }

    private void saveLog() {
        String mode = spinnerMode.getSelectedItem() != null ? spinnerMode.getSelectedItem().toString() : "N/A";
        String zone = spinnerZone.getSelectedItem() != null ? spinnerZone.getSelectedItem().toString() : "N/A"; // Get selected zone name
        String logKey = "session_logs_" + currentUserId;
        JSONObject log = new JSONObject();
        try {
            log.put("user", currentUserId);
            log.put("zone", zone);
            log.put("mode", mode);
            log.put("video", selectedVideoFile);
            log.put("resistance", resistanceLevel);
            log.put("reps", repetitionCount);
            log.put("duration", secondsPassed);
            log.put("timestamp", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            JSONArray logs = new JSONArray(prefs.getString(logKey, "[]"));
            logs.put(log);
            prefs.edit().putString(logKey, logs.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startInactivityTimer() {
        inactivityRunnable = () -> {
            Toast.makeText(this, "Inactive for 5 min. Redirecting...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        };
        resetInactivityTimer();
    }

    private void resetInactivityTimer() {
        handler.removeCallbacks(inactivityRunnable);
        handler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT_MS);
    }

    // Removed hasStoragePermission() and promptUserToSelectUsb() as USB URI is passed from MenuActivity

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        resetInactivityTimer();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(timerRunnable);
            handler.removeCallbacks(inactivityRunnable);
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
