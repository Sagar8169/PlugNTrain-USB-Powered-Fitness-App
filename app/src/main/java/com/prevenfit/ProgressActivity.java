package com.prevenfit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.textview.MaterialTextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProgressActivity extends AppCompatActivity {
    LinearLayout logContainer;
    SharedPreferences prefs;
    JSONArray logs; // Store logs as a class member for easier modification
    String currentUserDisplayName; // Store current user's display name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress); // Ensure this is your main layout

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> {
            exportLogsAsPdf();
        });

        logContainer = findViewById(R.id.logContainer);
        prefs = getSharedPreferences("PrevenFitPrefs", MODE_PRIVATE);

        // 🟡 Get current logged in user ID
        String userId = prefs.getString("user_id", "User");

        // ✅ Retrieve the current user's display name once
        currentUserDisplayName = prefs.getString("current_user_display_name_" + userId, "Guest User");
        if (currentUserDisplayName.equals("Guest User")) {
            // This case should ideally be handled by LoginActivity now, but as a fallback:
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("current_user_display_name_" + userId, "Default User Name");
            editor.apply();
            currentUserDisplayName = "Default User Name";
        }

        Toast.makeText(this, "Current User: " + currentUserDisplayName, Toast.LENGTH_LONG).show();
        loadLogs(); // Call a method to load and display logs
    }

    private void loadLogs() {
        logContainer.removeAllViews(); // Clear existing views
        // 🟡 Get current logged in user ID
        String userId = prefs.getString("user_id", "User");
        String logsJson = prefs.getString("session_logs_" + userId, "[]"); // ✅ User-specific logs
        try {
            logs = new JSONArray(logsJson);
            for (int i = logs.length() - 1; i >= 0; i--) {
                JSONObject log = logs.getJSONObject(i);
                addLogToView(log, i); // Pass the log object and index
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void exportLogsAsPdf() {
        String currentUserId = prefs.getString("user_id", "User");
        String logKey = "session_logs_" + currentUserId;
        String logsJson = prefs.getString(logKey, "[]");

        // currentUserDisplayName is already available as a class member

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int x = 40, y = 60;
        paint.setTextSize(16);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("Workout Progress Report for " + currentUserDisplayName, x, y, paint); // Use the display name here
        y += 30;
        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        try {
            JSONArray logs = new JSONArray(logsJson);
            for (int i = logs.length() - 1; i >= 0; i--) {
                JSONObject log = logs.getJSONObject(i);
                String userFromLog = log.optString("user", ""); // Use optString to safely get 'user'

                // ✅ Fallback: If 'user' from log is empty, use the current user's display name
                String userToDisplayInPdf = userFromLog.isEmpty() ? currentUserDisplayName : userFromLog;

                String zone = log.getString("zone");
                String mode = log.getString("mode");
                int reps = log.getInt("reps");
                int duration = log.getInt("duration");
                long timestamp = log.getLong("timestamp");
                String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(timestamp));

                // Display the user name (either from log or fallback)
                String entry = "📅 " + date + "\n👤 Logged By: " + userToDisplayInPdf + "\n📍 Zone: " + zone +
                        "\n⚙️ Mode: " + mode + "\n🏋️ Reps: " + reps + "\n⏱️ Duration: " + duration + " sec\n";

                for (String line : entry.split("\n")) {
                    if (y > 800) { // new page
                        pdfDocument.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 60;
                    }
                    canvas.drawText(line, x, y, paint);
                    y += 18;
                }
                y += 16;
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to create PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        pdfDocument.finishPage(page);
        File file = new File(getExternalFilesDir(null), "Workout_Progress_" + currentUserDisplayName.replace(" ", "_") + ".pdf"); // Add name to filename
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to write PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Share
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share PDF using"));
    }

    private void addLogToView(JSONObject log, final int originalIndex) throws JSONException {
        // String user = log.getString("user"); // No longer using this for display on card
        String zone = log.getString("zone");
        String mode = log.getString("mode");
        int reps = log.getInt("reps");
        int duration = log.getInt("duration");
        long timestamp = log.getLong("timestamp");
        String formattedDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(timestamp));

        LayoutInflater inflater = LayoutInflater.from(this);
        final View logEntryView = inflater.inflate(R.layout.item_log_entry, logContainer, false);

        // Find the MaterialTextViews in the inflated layout
        MaterialTextView tvTimestamp = logEntryView.findViewById(R.id.log_timestamp);
        MaterialTextView tvUser = logEntryView.findViewById(R.id.text_user);
        MaterialTextView tvZone = logEntryView.findViewById(R.id.text_zone);
        MaterialTextView tvMode = logEntryView.findViewById(R.id.text_mode);
        MaterialTextView tvReps = logEntryView.findViewById(R.id.text_reps);
        MaterialTextView tvDuration = logEntryView.findViewById(R.id.text_duration);
        ImageView deleteButton = logEntryView.findViewById(R.id.delete_log_button); // Find the delete button

        // Populate the views with data
        tvTimestamp.setText(formattedDate);
        // ✅ Set the user text to the currently logged-in user's display name
        tvUser.setText("User: " + currentUserDisplayName);
        tvZone.setText("Zone: " + zone);
        tvMode.setText("Mode: " + mode);
        tvReps.setText("Reps: " + reps);
        tvDuration.setText("Duration: " + duration + " sec");

        // Set OnClickListener for the delete button
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog(originalIndex, logEntryView);
            }
        });

        logContainer.addView(logEntryView);
    }

    private void showDeleteConfirmationDialog(final int indexToDelete, final View viewToRemove) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Log Entry")
                .setMessage("Are you sure you want to delete this log entry? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDelete(indexToDelete, viewToRemove);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(int indexToDelete, View viewToRemove) {
        try {
            logs.remove(indexToDelete);
            SharedPreferences.Editor editor = prefs.edit();
            String userId = prefs.getString("user_id", "User");
            editor.putString("session_logs_" + userId, logs.toString());
            editor.apply();
            logContainer.removeView(viewToRemove);
            loadLogs(); // Reload all logs to ensure correct indexing and display
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to delete log", Toast.LENGTH_SHORT).show();
        }
    }
}
