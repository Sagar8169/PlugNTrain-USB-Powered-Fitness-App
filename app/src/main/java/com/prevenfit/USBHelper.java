package com.prevenfit;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class USBHelper {

    // Read file from URI (e.g. menu.json from USB)
    public static String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            reader.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e("USBHelper", "Failed to read file: " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    // Helper to check if file is JSON
    public static boolean isJsonFile(Uri uri) {
        String path = uri.getPath();
        return path != null && path.endsWith(".json");
    }
}
