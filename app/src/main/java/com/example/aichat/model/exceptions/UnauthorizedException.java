package com.example.aichat.model.exceptions;

public class UnauthorizedException extends Exception{
    public UnauthorizedException(){
        super("Invalid or missing authentication credentials");
    }
}
