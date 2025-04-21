package com.example.aichat.model.entities;

import android.content.res.Resources;

import com.example.aichat.R;

import java.time.LocalDateTime;

public class MessageChat implements Comparable<MessageChat> {
    private Message message;
    private Chat chat;

    public Message getMessage() {
        return message;
    }

    public Chat getChat() {
        return chat;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public MessageChat(Message message, Chat chat){
        if (message!=null) {
            if (message.getChat() != chat.getId())
                throw new IllegalArgumentException("Message is not from chat");
            this.message = message;
        }
        this.chat =  chat;
    }
    public int compareTo(MessageChat other){
        return getTime().compareTo(other.getTime());
    }
    public LocalDateTime getTime(){
        LocalDateTime time = chat.getEndTimeFormat();
        if(time!=null) return time;
        if(message != null){
            return message.getTimeFormat();
        }
        return chat.getCreationTimeFormat();
    }
    public String getText(Resources resources){
        if(chat.isActive()) {
            if(message==null) return (String) resources.getText(R.string.chat_created);
            return message.getText();
        }
        return (String) resources.getText(R.string.chat_ended);
    }

}
