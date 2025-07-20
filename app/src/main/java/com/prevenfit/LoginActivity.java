package com.prevenfit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText editUserCode;
    EditText editUserName; // New: EditText for user name
    Button btnLogin;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUserCode = findViewById(R.id.editUserCode);
        editUserName = findViewById(R.id.editUserName); // Initialize the new EditText
        btnLogin = findViewById(R.id.btnLogin);
        prefs = getSharedPreferences("PrevenFitPrefs", Context.MODE_PRIVATE);

        // Auto skip if both user_id and display name are already saved
        String existingId = prefs.getString("user_id", null);
        String existingDisplayName = null;
        if (existingId != null) {
            existingDisplayName = prefs.getString("current_user_display_name_" + existingId, null);
        }

        if (existingId != null && existingDisplayName != null) {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
            return;
        } else if (existingId != null) {
            // If user ID exists but name doesn't, pre-fill code and prompt for name
            editUserCode.setText(existingId);
            Toast.makeText(this, "Please enter your name.", Toast.LENGTH_LONG).show();
        }

        btnLogin.setOnClickListener(v -> {
            String code = editUserCode.getText().toString().trim();
            String name = editUserName.getText().toString().trim(); // Get name from the new EditText

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                return; // Stop if name is empty
            }

            if (code.length() >= 3) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_id", code);
                editor.putString("current_user_display_name_" + code, name); // Save name with user_id
                editor.apply();

                Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MenuActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Enter valid code (at least 3 characters)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
