package com.example.servicemanagerapp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FCMHelper {

    // The deployed Firebase Cloud Function URL
    private static final String FCM_API = "https://us-central1-madservicemanagerapp.cloudfunctions.net/sendFCM";
    private static final String APP_SECRET = "SM_SECRET_2026";

    public static void sendNotification(String toTopic, String title, String messageStr, String serviceId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    URL url = new URL(FCM_API);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("User-Agent", "ServiceManagerApp");
                    conn.setRequestProperty("X-FCM-Secret", APP_SECRET);
                    conn.setRequestProperty("Content-Type", "application/json");

                    JSONObject json = new JSONObject();
                    json.put("to", "/topics/" + toTopic);

                    JSONObject info = new JSONObject();
                    info.put("title", title); // Notification title
                    info.put("body", messageStr); // Notification body
                    info.put("serviceId", serviceId);

                    json.put("notification", info);
                    json.put("data", info);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.close();

                    int respCode = conn.getResponseCode();
                    Log.d("FCM", "Response Code: " + respCode);

                } catch (Exception e) {
                    Log.e("FCM", "Error sending FCM message", e);
                }
                return null;
            }
        }.execute();
    }
}
