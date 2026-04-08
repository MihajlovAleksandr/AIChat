package com.example.aichat.model.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.aichat.model.utils.JsonHelper;

@Entity(tableName = "PendingCommands")
public class PendingCommand {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String command;

    public PendingCommand(WSSCommand WSSCommand) {
        this.command = JsonHelper.Serialize(WSSCommand);
    }

    public PendingCommand() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String commandJson) {
        this.command = commandJson;
    }

    public WSSCommand getCommandFormat() {
        return JsonHelper.Deserialize(command, WSSCommand.class);
    }
}
