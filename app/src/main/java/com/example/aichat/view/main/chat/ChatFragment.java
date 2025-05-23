package com.example.aichat.view.main.chat;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.R;
import com.example.aichat.controller.main.chat.ChatFragmentController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.database.DatabaseManager;
import com.example.aichat.model.entities.Chat;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.User;
import com.example.aichat.view.main.MainActivity;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView rvMessages;
    private EditText tiMessage;
    private Button bSendMessage;
    private MessageAdapter messageAdapter;
    private ChatFragmentController controller;
    private int chatId;
    private int currentUserId;
    private ConnectionManager connectionManager;
    private View rootView;
    private FragmentActivity activity;
    private ImageButton btnOptions;
    private ChatMembersAdapter membersAdapter;
    private View membersPanel;
    private View invisibleClickArea;
    ImageButton btnBack;
    private View searchPanel;
    private View resultSearchPanel;
    private EditText searchET;
    private List<Message> foundMessages;
    private int foundMessageNumber;

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    public ChatFragment(ConnectionManager connectionManager, int chatId, int currentUserId) {
        this.connectionManager = connectionManager;
        this.chatId = chatId;
        this.currentUserId = currentUserId;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        rvMessages = rootView.findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        btnBack = rootView.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> navigateBack());

        tiMessage = rootView.findViewById(R.id.ti_message);
        bSendMessage = rootView.findViewById(R.id.b_send_message);

        controller = new ChatFragmentController(this, connectionManager, chatId, currentUserId);

        new LoadChatAndMessagesTask().execute(chatId);

        bSendMessage.setOnClickListener(v -> {
            String messageText = tiMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                controller.sendMessage(messageText);
                tiMessage.setText("");
            }
        });

        btnOptions = rootView.findViewById(R.id.btn_options);
        btnOptions.setOnClickListener(v -> {
            if (activity == null) return;

            PopupMenu popupMenu = new PopupMenu(activity, v);
            popupMenu.getMenuInflater().inflate(R.menu.chat_options_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_end_chat) {
                    controller.endChat();
                    return true;
                } else if (id == R.id.menu_view_members) {
                    toggleMembersPanel();
                    return true;
                } else if (id == R.id.menu_search_words) {
                    toggleSearchPanel(true);
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        RecyclerView membersRecyclerView = rootView.findViewById(R.id.membersRecyclerView);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        membersAdapter = new ChatMembersAdapter(new ArrayList<>(), currentUserId);
        membersRecyclerView.setAdapter(membersAdapter);

        membersPanel = rootView.findViewById(R.id.membersPanel);
        invisibleClickArea = rootView.findViewById(R.id.invisibleClickArea);
        invisibleClickArea.setOnClickListener(v -> toggleMembersPanel());

        ImageButton btnBackMembers = rootView.findViewById(R.id.btn_back_members);
        btnBackMembers.setOnClickListener(v -> toggleMembersPanel());
        searchPanel = rootView.findViewById(R.id.search_layout);
        resultSearchPanel = rootView.findViewById(R.id.searchResult_layout);
        ImageButton backSearchBtn = rootView.findViewById(R.id.search_btn_back);
        backSearchBtn.setOnClickListener(v->toggleSearchPanel(false));
        ImageButton backSearchResultBtn = rootView.findViewById(R.id.searchResult_btn_back);
        backSearchResultBtn.setOnClickListener(v->hideResultSearchPanel());
        ImageButton searchActionBtn = rootView.findViewById(R.id.btn_search_action);
        searchET = rootView.findViewById(R.id.et_search) ;
        searchActionBtn.setOnClickListener(v->showSearchResult());
        ImageButton upButton =rootView.findViewById(R.id.btn_top);
        upButton.setOnClickListener(v->changeFoundMessageNumber(foundMessageNumber-1));
        ImageButton downButton =rootView.findViewById(R.id.btn_bottom);
        downButton.setOnClickListener(v->changeFoundMessageNumber(foundMessageNumber+1));
        return rootView;
    }
    public void updateOnlineState(int id, boolean isOnline){
        activity.runOnUiThread(()-> {
            membersAdapter.updateOnlineState(id, isOnline);
        });
    }
    private void toggleMembersPanel() {
        if (membersPanel == null || invisibleClickArea == null) return;

        if (membersPanel.getVisibility() == View.VISIBLE) {
            membersPanel.setVisibility(View.GONE);
            invisibleClickArea.setVisibility(View.GONE);
        } else {
            membersPanel.setVisibility(View.VISIBLE);
            invisibleClickArea.setVisibility(View.VISIBLE);
        }
    }
    private void showSearchResult(){
        controller.findMessages(searchET.getText().toString());
    }
    private void hideResultSearchPanel(){
        searchPanel.setVisibility(View.VISIBLE);
        resultSearchPanel.setVisibility(View.GONE);
    }
    public void sendMessage(Message message) {
        if (activity != null && messageAdapter != null) {
            activity.runOnUiThread(() -> {messageAdapter.addMessage(message); });
        }
    }
    public void toggleSearchPanel(boolean needToShow){
        if(needToShow){
            searchPanel.setVisibility(View.VISIBLE);
            btnOptions.setEnabled(false);
            btnBack.setEnabled(false);

        }
        else{
            searchPanel.setVisibility(View.GONE);
            btnOptions.setEnabled(true);
            btnBack.setEnabled(true);
        }
    }

    private void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).backToChats();
        }
    }

    public void endChat() {
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (bSendMessage != null) bSendMessage.setEnabled(false);
                if (tiMessage != null) {
                    tiMessage.setText(activity.getText(R.string.chat_ended));
                    tiMessage.setEnabled(false);
                }
                if (btnOptions != null) btnOptions.setEnabled(false);
            });
        }
    }

    public void loadUsers(List<User> users) {
        if (membersAdapter != null) {
            membersAdapter.updateMembers(users);
        }
    }

    @Override
    public void onDestroy() {
        controller.destroy();
        super.onDestroy();
    }

    private class LoadChatAndMessagesTask extends AsyncTask<Integer, Void, ChatAndMessages> {
        @Override
        protected ChatAndMessages doInBackground(Integer... params) {
            int chatId = params[0];
            List<Message> messages = DatabaseManager.getDatabase().messageDao().getMessagesByChatId(chatId);
            Chat chat = DatabaseManager.getDatabase().chatDao().getChatById(chatId);
            return new ChatAndMessages(chat, messages);
        }

        @Override
        protected void onPostExecute(ChatAndMessages result) {
            if (result == null) return;

            if (result.chat != null){
                if(result.chat.getEndTime() != null){
                    endChat();
                }
                else {
                    controller.loadUsers();
                }
            }

            if (result.messages != null) {
                messageAdapter = new MessageAdapter(currentUserId, result.messages);
                rvMessages.setAdapter(messageAdapter);
                if (!result.messages.isEmpty()) {
                    rvMessages.scrollToPosition(result.messages.size() - 1);
                }
            }
        }
    }

    public void findMessages(List<Message> messages,String text) {
        activity.runOnUiThread(() ->
        {
            foundMessageNumber=0;
            TextView textView = resultSearchPanel.findViewById(R.id.tv_title);
            textView.setText(text);
            foundMessages = messages;
            changeFoundMessageNumber(0);
            resultSearchPanel.setVisibility(View.VISIBLE);
            searchPanel.setVisibility(View.GONE);
        });
    }
    private void changeFoundMessageNumber(int position){
        if(position>=0 && position<foundMessages.size()){
            foundMessageNumber=position;
            if(!foundMessages.isEmpty()){
                messageAdapter.findMessages(foundMessages.get(foundMessageNumber));
            }
        }
        TextView countView =resultSearchPanel.findViewById(R.id.tv_searchResultCount);
        countView.setText((foundMessageNumber+1)+"/"+foundMessages.size());

    }
    private static class ChatAndMessages {
        Chat chat;
        List<Message> messages;
        ChatAndMessages(Chat chat, List<Message> messages) {
            this.chat = chat;
            this.messages = messages;
        }
    }

    private static class ChatMembersAdapter extends RecyclerView.Adapter<ChatMembersAdapter.ChatMemberViewHolder> {
        private List<User> members;
        private OnMemberClickListener listener;
        private int currentUserId;

        interface OnMemberClickListener {
            void onMemberClick(User member);
        }

        ChatMembersAdapter(List<User> members, int currentUserId) {
            this.members = members;
            this.currentUserId = currentUserId;
        }

        void setOnMemberClickListener(OnMemberClickListener listener) {
            this.listener = listener;
        }

        void updateMembers(List<User> newMembers) {
            this.members = newMembers != null ? newMembers : new ArrayList<>();
            notifyDataSetChanged();
        }
        public void updateOnlineState(int id, boolean isOnline){
            for (User user : members) {
                if (user.getId() == id) {
                    user.setOnline(isOnline);
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        @NonNull
        @Override
        public ChatMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_member_item, parent, false);
            return new ChatMemberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatMemberViewHolder holder, int position) {
            if (members == null || position < 0 || position >= members.size()) return;

            User member = members.get(position);
            holder.bind(member, currentUserId);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberClick(member);
                } else {
                    Toast.makeText(v.getContext(), member.getUserData().getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return members != null ? members.size() : 0;
        }

        static class ChatMemberViewHolder extends RecyclerView.ViewHolder {
            private final TextView memberName;
            private final TextView memberDetails;
            private final View onlineStatus;
            private final Resources resources;

            ChatMemberViewHolder(@NonNull View itemView) {
                super(itemView);
                memberName = itemView.findViewById(R.id.memberName);
                onlineStatus = itemView.findViewById(R.id.onlineStatus);
                memberDetails = itemView.findViewById(R.id.memberDetails);
                resources = itemView.getContext().getResources();
            }

            void bind(User member, int currentUserId) {
                if (member == null || member.getUserData() == null) {
                    return;
                }

                String name = member.getUserData().getName();
                if (member.getId() == currentUserId) {
                    name += " (" + resources.getString(R.string.you) + ")";
                }
                memberName.setText(name);

                String details = String.format("%s %s",
                        member.getUserData().getGender(),
                        member.getUserData().getAge());
                memberDetails.setText(details);

                if (onlineStatus != null) {
                    onlineStatus.setVisibility(member.isOnline() ? View.VISIBLE : View.GONE);
                }
            }
        }
    }
}