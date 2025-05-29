package com.example.aichat.model.notifications;

import android.util.Log;

import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.entities.Command;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String chatIdStr = remoteMessage.getData().get("chatId");
            String isBodyPrompt = remoteMessage.getData().get("isBodyPrompt");

            try {
                int chatId = (chatIdStr != null) ? Integer.parseInt(chatIdStr) : -1;
                boolean isBodyPromptBool = isBodyPrompt != null && Boolean.parseBoolean(isBodyPrompt);

                if (isBodyPromptBool && body != null) {
                    try {
                        int resId = getResources().getIdentifier(body, "string", getPackageName());
                        if (resId != 0) {
                            body = getString(resId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting string resource", e);
                    }
                }

                if (title != null && body != null) {
                    NotificationHelper notificationHelper = NotificationSingleton.getInstance().getNotificationHelper();
                    if(notificationHelper==null){
                        notificationHelper = new NotificationHelper();
                        NotificationSingleton.getInstance().setNotificationHelper(notificationHelper);
                        Log.d(TAG, "Create new NotificationHelper");
                    }
                    notificationHelper.sendNotification(this, title, body, chatId);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid chatId format", e);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        SecurePreferencesManager.saveNotificationToken(this, token);
        sendRegistrationTokenToServer(token);
    }

    public static void sendRegistrationTokenToServer(String token) {
        if (token != null) {
            Command command = new Command("UpdateNotificationToken");
            command.addData("token", token);
            ConnectionSingleton.getInstance().getConnectionManager().SendCommand(command);
        }
    }
}