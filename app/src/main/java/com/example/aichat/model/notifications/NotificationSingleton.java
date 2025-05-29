package com.example.aichat.model.notifications;

public class NotificationSingleton {
    public static void init(){
        if(instance.notificationHelper==null)
            instance.setNotificationHelper(new NotificationHelper());
    }
    private static final NotificationSingleton instance = new NotificationSingleton();
    private NotificationHelper notificationHelper;

    private NotificationSingleton() {}

    public static NotificationSingleton getInstance() {
        return instance;
    }

    public NotificationHelper getNotificationHelper() {
        return notificationHelper;
    }

    public void setNotificationHelper(NotificationHelper notificationHelper) {
        this.notificationHelper = notificationHelper;
    }
}
