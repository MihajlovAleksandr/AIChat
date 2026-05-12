package com.example.aichat.model.connection;

import android.app.Activity;

import com.example.aichat.view.main.MainActivity;

public class ConnectionSingleton {
    private static final ConnectionSingleton instance = new ConnectionSingleton();

    private static boolean isInitialized = false;
    private static ConnectionDispatcher dispatcher;

    public static void init(Activity activity){
        if(activity.getClass() != MainActivity.class)
            throw new IllegalArgumentException("Only Main Activity can initialize Connection Singleton");
        dispatcher = new ConnectionDispatcher(new TokenStorage(activity));
        isInitialized = true;
    }

    public static ConnectionSingleton getInstance(){
        return instance;
    }

    public ConnectionDispatcher getConnectionDispatcher(){
        if(!isInitialized)
            throw new RuntimeException("Connection dispatcher is not initialized");
        return dispatcher;
    }

    private ConnectionSingleton(){}
}
