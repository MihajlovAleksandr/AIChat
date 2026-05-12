package com.example.aichat.model.exceptions;

public class SignalRNotConnectedException extends RuntimeException{
    public SignalRNotConnectedException(){
        super("Trying to use SignalR without active connection");
    }
}
