package com.example.aichat;

import java.util.EventObject;

public class GetCommandEvent extends EventObject {
    private Command command;

    public GetCommandEvent(Object source, Command command) {
        super(source);
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }
}
