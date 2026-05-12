package com.example.aichat.model.connection;

import android.util.Base64;
import org.json.JSONObject;

public class JwtUtils {

    public static JSONObject decodePayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;

            String payload = parts[1];
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String decoded = new String(decodedBytes, "UTF-8");

            return new JSONObject(decoded);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

