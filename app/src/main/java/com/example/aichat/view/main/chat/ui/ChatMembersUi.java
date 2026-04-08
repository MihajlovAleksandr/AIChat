package com.example.aichat.view.main.chat.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.model.entities.User;
import com.example.aichat.view.main.chat.ChatFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatMembersUi {

    private final ChatFragment fragment;
    private final View root;

    private final RecyclerView rvMembers;
    private final MembersAdapter adapter;

    private UUID searchingChatId = null;

    public ChatMembersUi(View root, ChatFragment fragment) {
        this.root = root;
        this.fragment = fragment;

        rvMembers = root.findViewById(R.id.membersRecyclerView);
        rvMembers.setLayoutManager(new LinearLayoutManager(root.getContext()));

        adapter = new MembersAdapter(new ArrayList<>(), fragment);
        rvMembers.setAdapter(adapter);
    }
    public void updateMembers(List<User> users) {
        adapter.updateMembers(users);
    }

    public void updateOnline(UUID userId, boolean isOnline) {
        adapter.updateOnlineState(userId, isOnline);
    }

    public void setSearchingChatId(UUID id) {
        this.searchingChatId = id;
        adapter.setSearchingChatId(id);
    }

    public List<UUID> getUserIds() {
        List<UUID> ids = new ArrayList<>();
        for (User u : adapter.getMembers()) {
            ids.add(u.getId());
        }
        return ids;
    }

    private static class MembersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final ChatFragment fragment;
        private List<User> members;

        private UUID searchingChatId = null;

        private static final int TYPE_MEMBER = 0;
        private static final int TYPE_ADD_BUTTON = 1;
        private static final int TYPE_SEARCH_CANCEL = 2;

        MembersAdapter(List<User> members, ChatFragment fragment) {
            this.members = members != null ? members : new ArrayList<>();
            this.fragment = fragment;
        }

        void updateMembers(List<User> newMembers) {
            this.members = newMembers != null ? newMembers : new ArrayList<>();
            notifyDataSetChanged();
        }

        void updateOnlineState(UUID id, boolean isOnline) {
            for (int i = 0; i < members.size(); i++) {
                User u = members.get(i);
                if (u.getId().equals(id)) {
                    u.setOnline(isOnline);
                    notifyItemChanged(i);
                    return;
                }
            }
        }

        void setSearchingChatId(UUID id) {
            this.searchingChatId = id;
            notifyDataSetChanged();
        }

        List<User> getMembers() {
            return members;
        }

        @Override
        public int getItemViewType(int position) {
            if (searchingChatId != null && position == members.size()) {
                return TYPE_SEARCH_CANCEL;
            }
            if (position < members.size()) {
                return TYPE_MEMBER;
            }
            return TYPE_ADD_BUTTON;
        }

        @Override
        public int getItemCount() {
            int base = members.size();
            if (searchingChatId != null) {
                return base + 1;
            }
            return base + 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            if (viewType == TYPE_MEMBER) {
                View view = inflater.inflate(R.layout.chat_member_item, parent, false);
                return new MemberViewHolder(view);
            }

            if (viewType == TYPE_ADD_BUTTON) {
                View view = inflater.inflate(R.layout.add_member_button_item, parent, false);
                return new AddMemberViewHolder(view);
            }

            View view = inflater.inflate(R.layout.chat_search_cancel_item, parent, false);
            return new SearchCancelViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof MemberViewHolder) {
                User user = members.get(position);
                MemberViewHolder vh = (MemberViewHolder) holder;

                vh.bind(user);

                vh.itemView.setOnClickListener(v -> fragment.onMemberClick(user));
                vh.removeButton.setOnClickListener(v -> fragment.onRemoveMemberClick(user.getId()));
            } else if (holder instanceof AddMemberViewHolder) {
                holder.itemView.setOnClickListener(v -> fragment.onAddMemberClick());
            } else if (holder instanceof SearchCancelViewHolder) {
                ((SearchCancelViewHolder) holder).bind(searchingChatId, fragment);
            }
        }

        private static class MemberViewHolder extends RecyclerView.ViewHolder {

            final TextView name;
            final TextView details;
            final View onlineStatus;
            final ImageButton removeButton;

            MemberViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.memberName);
                details = itemView.findViewById(R.id.memberDetails);
                onlineStatus = itemView.findViewById(R.id.onlineStatus);
                removeButton = itemView.findViewById(R.id.removeButton);
            }

            void bind(User user) {
                name.setText(user.getUserData().getName());

                details.setText(itemView.getContext().getString(
                        R.string.member_details,
                        user.getUserData().getGender(),
                        user.getUserData().getAge()
                ));

                onlineStatus.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);
            }
        }

        private static class AddMemberViewHolder extends RecyclerView.ViewHolder {
            AddMemberViewHolder(View itemView) {
                super(itemView);
            }
        }

        private static class SearchCancelViewHolder extends RecyclerView.ViewHolder {

            private final TextView text;
            private final Button cancel;

            SearchCancelViewHolder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.search_text);
                cancel = itemView.findViewById(R.id.cancel_button);
            }

            void bind(UUID searchingChatId, ChatFragment fragment) {
                if (searchingChatId == null) {
                    text.setText(R.string.searching_new_group);
                } else {
                    text.setText(R.string.searching_chat_user);
                }

                cancel.setOnClickListener(v -> fragment.onCancelSearchClick());
            }
        }
    }
}
