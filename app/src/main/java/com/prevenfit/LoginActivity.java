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
    Button btnLogin;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUserCode = findViewById(R.id.editUserCode);
        btnLogin = findViewById(R.id.btnLogin);
        prefs = getSharedPreferences("PrevenFitPrefs", Context.MODE_PRIVATE);

        // Auto skip if already saved
        String existingId = prefs.getString("user_id", null);
        if (existingId != null) {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String code = editUserCode.getText().toString().trim();
            if (code.length() >= 3) {
                prefs.edit().putString("user_id", code).apply();
                startActivity(new Intent(this, MenuActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Enter valid code", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
