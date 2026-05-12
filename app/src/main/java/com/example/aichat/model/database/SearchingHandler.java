package com.example.aichat.model.database;

import java.util.ArrayList;
import java.util.List;

public class SearchingHandler {
    private boolean isChatSearching;
    private GroupSearchModel groupSearchModel;
    private final List<ChatSearchingStatusChangedListener> chatSearchingStatusChangedListeners = new ArrayList<>();
    private final List<GroupSearchingStatusChangedListener> groupSearchingStatusChangedListeners = new ArrayList<>();

    public SearchingHandler(){
        isChatSearching = false;
        groupSearchModel = new GroupSearchModel();
    }

    public void setChatSearching(boolean chatSearching) {
        isChatSearching = chatSearching;
        notifyChatSearchingStatusChanged(chatSearching);
    }

    public void setGroupSearchModel(GroupSearchModel groupSearchModel) {
        this.groupSearchModel = groupSearchModel;
        notifyGroupSearchingStatusChanged(groupSearchModel);
    }

    public GroupSearchModel getGroupSearchModel() {
        return groupSearchModel;
    }

    public boolean getIsChatSearching(){
        return isChatSearching;
    }

    private void notifyChatSearchingStatusChanged(boolean isChatSearching){
        for (var listener : chatSearchingStatusChangedListeners) {
            listener.handle(isChatSearching);
        }
    }

    private void notifyGroupSearchingStatusChanged(GroupSearchModel  groupSearchingModel){
        for (var listener : groupSearchingStatusChangedListeners) {
            listener.handle(groupSearchingModel);
        }
    }

    public void addChatSearchingStatusChangedListener(ChatSearchingStatusChangedListener listener){
        chatSearchingStatusChangedListeners.add(listener);
    }

    public void addGroupSearchingStatusChangedListener(GroupSearchingStatusChangedListener listener){
        groupSearchingStatusChangedListeners.add(listener);
    }

    public void removeChatSearchingStatusChangedListener(ChatSearchingStatusChangedListener listener){
        chatSearchingStatusChangedListeners.remove(listener);
    }

    public void removeGroupSearchingStatusChangedListener(GroupSearchingStatusChangedListener listener){
        groupSearchingStatusChangedListeners.remove(listener);
    }

}
