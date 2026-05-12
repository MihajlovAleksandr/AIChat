package com.example.aichat.model.database;
public class ChatStatusSingleton {

    private final SearchingHandler handler;
    private final static ChatStatusSingleton instance = new ChatStatusSingleton();

    private ChatStatusSingleton(){
        handler = new SearchingHandler();
    }

    public static ChatStatusSingleton getInstance(){
        return instance;
    }

    public SearchingHandler getHandler(){
        return handler;
    }
}
