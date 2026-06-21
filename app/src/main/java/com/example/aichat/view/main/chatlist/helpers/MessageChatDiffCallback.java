package com.example.aichat.view.main.chatlist.helpers;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageChat;
import java.util.List;
import java.util.Objects;

public class MessageChatDiffCallback extends DiffUtil.Callback {

    private final List<MessageChat> oldList;
    private final List<MessageChat> newList;

    public MessageChatDiffCallback(List<MessageChat> oldList, List<MessageChat> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() { return oldList.size(); }

    @Override
    public int getNewListSize() { return newList.size(); }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        MessageChat o = oldList.get(oldPos);
        MessageChat n = newList.get(newPos);

        if (o == null || n == null) return o == n;

        return Objects.equals(o.getChat().getId(), n.getChat().getId());
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        MessageChat o = oldList.get(oldPos);
        MessageChat n = newList.get(newPos);

        if (o == null || n == null) return o == n;

        Message om = o.getMessage();
        Message nm = n.getMessage();

        return Objects.equals(o.getChat().getName(), n.getChat().getName())
                && Objects.equals(om == null ? null : om.getText(), nm == null ? null : nm.getText())
                && Objects.equals(o.getTime(), n.getTime())
                && o.getUnreadMessagesCount() == n.getUnreadMessagesCount()
                && o.isEnded() == n.isEnded()
                && o.getChat().isPinned() == n.getChat().isPinned();
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return new Object();
    }
}
