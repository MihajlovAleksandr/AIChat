package com.example.aichat.view.main.chatlist.helpers;

import com.example.aichat.model.entities.MessageChat;
import java.util.Comparator;

public class MessageChatComparator implements Comparator<MessageChat> {
    @Override
    public int compare(MessageChat a, MessageChat b) {

        boolean aPinned = a.getChat().isPinned();
        boolean bPinned = b.getChat().isPinned();

        if (aPinned && !bPinned) return -1;
        if (!aPinned && bPinned) return 1;

        return b.getTime().compareTo(a.getTime());
    }
}

