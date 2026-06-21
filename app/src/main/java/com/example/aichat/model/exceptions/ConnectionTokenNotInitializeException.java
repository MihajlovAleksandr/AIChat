package com.example.aichat.model.exceptions;

public class ConnectionTokenNotInitializeException extends RuntimeException {
    public ConnectionTokenNotInitializeException(){
        super("Trying to connect without token");
    }
}
