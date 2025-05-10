package com.example.aichat.controller.main.chatlist;

import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    public static String getFormattedTime(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        if (now.toLocalDate().equals(time.toLocalDate())) {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (time.isAfter(now.minusDays(7))) {
            return time.format(DateTimeFormatter.ofPattern("EE"));
        } else if (time.isAfter(now.minusYears(1))) {
            return time.format(DateTimeFormatter.ofPattern("dd.MM"));
        } else {
            return time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }


    public static int getStatusIcon(Chat chat) {
        return chat.isActive() ?
                android.R.drawable.presence_online :
                android.R.drawable.presence_invisible;
    }
    public static List<Message> getLastMessages(Message[] messages){
        List<Message> messageList =new ArrayList<>();
        if(messages.length>0) {
            messageList.add(messages[messages.length - 1]);
            for (int i = messages.length - 2; i >= 0; i--) {
                if(messages[i].getChat()!=messages[i+1].getChat())
                {
                    messageList.add(messages[i]);
                }
            }
        }
        return   messageList;
    }
}
