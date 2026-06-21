package com.example.aichat.view.main.chat.helpers;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aichat.model.entities.ChatType;
import com.example.aichat.model.entities.User;
import com.example.aichat.R;
import com.example.aichat.view.main.chat.ChatFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatMembersUi {

    public static final UUID NEW_GROUP_SEARCH =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ChatFragment fragment;

    private final View membersPanel;
    private final View invisibleClickArea;
    private final RecyclerView rvMembers;

    private final ImageButton btnBackMembers;
    private final ImageButton btnMembersMenu;
    private final ImageButton btnOptions;

    private final MembersAdapter adapter;

    private UUID searchingChatId = null;

    private int btnOptionsVisibilityBeforePanel = View.VISIBLE;
    private boolean panelAnimationRunning = false;

    public ChatMembersUi(View root, ChatFragment fragment) {
        this.fragment = fragment;

        membersPanel = root.findViewById(R.id.membersPanel);
        invisibleClickArea = root.findViewById(R.id.invisibleClickArea);
        rvMembers = root.findViewById(R.id.membersRecyclerView);

        btnBackMembers = root.findViewById(R.id.btn_back_members);
        btnMembersMenu = root.findViewById(R.id.btn_members_menu);
        btnOptions = root.findViewById(R.id.btn_options);

        UUID currentUserId = fragment != null ? fragment.getCurrentUserId() : null;

        if (rvMembers != null) {
            rvMembers.setLayoutManager(new LinearLayoutManager(root.getContext()));
            rvMembers.setItemAnimator(UiAnimations.createMembersPanelItemAnimator());

            adapter = new MembersAdapter(
                    new ArrayList<>(),
                    fragment,
                    rvMembers,
                    currentUserId
            );

            rvMembers.setAdapter(adapter);
        } else {
            adapter = new MembersAdapter(
                    new ArrayList<>(),
                    fragment,
                    null,
                    currentUserId
            );
        }

        setupPanelClicks();
    }

    private void setupPanelClicks() {
        if (membersPanel != null) {
            membersPanel.setClickable(true);
            membersPanel.setFocusable(true);

            membersPanel.setOnClickListener(v -> {
            });
        }

        if (invisibleClickArea != null) {
            invisibleClickArea.setClickable(true);
            invisibleClickArea.setFocusable(true);
            invisibleClickArea.setOnClickListener(v -> hidePanel());
        }

        if (btnBackMembers != null) {
            btnBackMembers.setOnClickListener(v ->
                    UiAnimations.animateSmallButtonClick(v, this::hidePanel)
            );
        }

        if (btnMembersMenu != null) {
            btnMembersMenu.setVisibility(View.GONE);
            btnMembersMenu.setOnClickListener(null);
        }
    }

    public void togglePanel() {
        runOnUiThread(() -> {
            if (panelAnimationRunning) {
                return;
            }

            if (isPanelVisible()) {
                hidePanelInternal(true);
                return;
            }

            if (!canOpenMembersPanel()) {
                showPanelUnavailableToast();
                return;
            }

            showPanelInternal(true);
        });
    }

    public void showPanel() {
        runOnUiThread(() -> {
            if (!canOpenMembersPanel()) {
                showPanelUnavailableToast();
                return;
            }

            showPanelInternal(true);
        });
    }

    public void hidePanel() {
        runOnUiThread(() -> hidePanelInternal(true));
    }

    public void hidePanelImmediately() {
        runOnUiThread(() -> hidePanelInternal(false));
    }

    private void showPanelInternal(boolean animated) {
        if (membersPanel == null || isPanelVisible() || panelAnimationRunning) {
            return;
        }

        if (!canOpenMembersPanel()) {
            showPanelUnavailableToast();
            return;
        }

        panelAnimationRunning = true;

        if (btnOptions != null) {
            btnOptionsVisibilityBeforePanel = btnOptions.getVisibility();

            if (btnOptions.getVisibility() == View.VISIBLE) {
                UiAnimations.animateOptionsButtonHide(btnOptions, null);
            } else {
                btnOptions.setVisibility(View.GONE);
            }
        }

        if (btnMembersMenu != null) {
            btnMembersMenu.setVisibility(View.GONE);
        }

        if (!animated) {
            membersPanel.setVisibility(View.VISIBLE);
            membersPanel.setAlpha(1f);
            membersPanel.setTranslationY(0f);
            membersPanel.setClickable(true);
            membersPanel.setFocusable(true);

            if (invisibleClickArea != null) {
                invisibleClickArea.setVisibility(View.VISIBLE);
                invisibleClickArea.setAlpha(1f);
                invisibleClickArea.setClickable(true);
                invisibleClickArea.setFocusable(true);
            }

            panelAnimationRunning = false;
            return;
        }

        UiAnimations.animateMembersPanelOpen(
                membersPanel,
                invisibleClickArea,
                () -> panelAnimationRunning = false
        );
    }

    private void hidePanelInternal(boolean animated) {
        if (membersPanel == null || membersPanel.getVisibility() != View.VISIBLE) {
            UiAnimations.hideMembersPanelImmediately(membersPanel, invisibleClickArea);
            restoreOptionsButton();
            panelAnimationRunning = false;
            return;
        }

        if (panelAnimationRunning) {
            return;
        }

        panelAnimationRunning = true;

        if (!animated) {
            UiAnimations.hideMembersPanelImmediately(membersPanel, invisibleClickArea);
            restoreOptionsButton();
            panelAnimationRunning = false;
            return;
        }

        UiAnimations.animateMembersPanelClose(
                membersPanel,
                invisibleClickArea,
                () -> {
                    restoreOptionsButton();
                    panelAnimationRunning = false;
                }
        );
    }

    private void restoreOptionsButton() {
        if (btnOptions == null) {
            return;
        }

        if (btnOptionsVisibilityBeforePanel == View.VISIBLE) {
            UiAnimations.animateOptionsButtonShow(btnOptions);
        } else {
            btnOptions.animate().cancel();
            btnOptions.setVisibility(btnOptionsVisibilityBeforePanel);
            btnOptions.setAlpha(1f);
            btnOptions.setScaleX(1f);
            btnOptions.setScaleY(1f);
        }
    }

    public boolean isPanelVisible() {
        return membersPanel != null && membersPanel.getVisibility() == View.VISIBLE;
    }

    public void updateMembers(List<User> users) {
        runOnUiThread(() -> {
            UUID creatorId = resolveCreatorId(users);
            boolean currentUserIsCreator = isCurrentUserCreator(creatorId);

            adapter.updateMembers(
                    users,
                    creatorId,
                    currentUserIsCreator,
                    canAddMembersToCurrentChat(),
                    canRemoveMembersFromCurrentChat()
            );
        });
    }

    public void updateOnline(UUID userId, @Nullable String lastOnline) {
        runOnUiThread(() -> adapter.updateOnlineState(userId, lastOnline));
    }

    public void setSearchState(UUID chatId) {
        runOnUiThread(() -> {
            searchingChatId = chatId;
            adapter.setSearchingChatId(chatId);
        });
    }

    public boolean isNoSearch() {
        return searchingChatId == null;
    }

    public boolean isSearchingNewGroup() {
        return NEW_GROUP_SEARCH.equals(searchingChatId);
    }

    public boolean isSearchingExistingGroup() {
        return searchingChatId != null && !NEW_GROUP_SEARCH.equals(searchingChatId);
    }

    public UUID getSearchingChatId() {
        return searchingChatId;
    }

    public List<UUID> getUserIds() {
        return adapter.getMemberIds();
    }

    private boolean canOpenMembersPanel() {
        if (fragment == null || fragment.getChat() == null) {
            return false;
        }

        ChatType type = fragment.getCurrentChatType();

        return type != ChatType.AI && type != ChatType.Random;
    }

    private boolean canAddMembersToCurrentChat() {
        if (fragment == null || fragment.getChat() == null) {
            return false;
        }

        return fragment.getCurrentChatType() == ChatType.Group;
    }

    private boolean canRemoveMembersFromCurrentChat() {
        if (fragment == null || fragment.getChat() == null) {
            return false;
        }

        return fragment.getCurrentChatType() == ChatType.Group;
    }

    private void showPanelUnavailableToast() {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }

        Toast.makeText(
                fragment.getContext(),
                "Панель участников недоступна для этого типа чата",
                Toast.LENGTH_SHORT
        ).show();
    }

    private UUID resolveCreatorId(List<User> users) {
        if (fragment != null) {
            UUID creatorId = fragment.getChatCreatorId();

            if (creatorId != null) {
                return creatorId;
            }
        }

        if (users != null && !users.isEmpty() && users.get(0) != null) {
            return users.get(0).getId();
        }

        return null;
    }

    private boolean isCurrentUserCreator(UUID creatorId) {
        if (fragment == null || creatorId == null) {
            return false;
        }

        UUID currentUserId = fragment.getCurrentUserId();

        return currentUserId != null && currentUserId.equals(creatorId);
    }

    private void runOnUiThread(Runnable action) {
        if (action == null) return;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
            return;
        }

        View postTarget = rvMembers != null ? rvMembers : membersPanel;

        if (postTarget != null) {
            postTarget.post(action);
        }
    }

    private static class MembersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_MEMBER = 0;
        private static final int TYPE_ADD_BUTTON = 1;
        private static final int TYPE_SEARCH_CANCEL = 2;

        private final ChatFragment fragment;
        private final RecyclerView recyclerView;
        private final UUID currentUserId;

        private List<User> members;
        private UUID searchingChatId = null;
        private UUID creatorUserId = null;
        private boolean currentUserIsCreator = false;
        private boolean canAddMembers = false;
        private boolean canRemoveMembers = false;

        MembersAdapter(
                List<User> members,
                ChatFragment fragment,
                RecyclerView recyclerView,
                UUID currentUserId
        ) {
            this.members = members != null ? members : new ArrayList<>();
            this.fragment = fragment;
            this.recyclerView = recyclerView;
            this.currentUserId = currentUserId;
        }

        void updateMembers(
                List<User> newMembers,
                @Nullable UUID creatorUserId,
                boolean currentUserIsCreator,
                boolean canAddMembers,
                boolean canRemoveMembers
        ) {
            this.members = newMembers != null ? new ArrayList<>(newMembers) : new ArrayList<>();
            this.creatorUserId = creatorUserId;
            this.currentUserIsCreator = currentUserIsCreator;
            this.canAddMembers = canAddMembers;
            this.canRemoveMembers = canRemoveMembers;

            notifyDataSetChangedSafe();
        }

        void updateOnlineState(UUID id, @Nullable String lastOnline) {
            if (id == null) return;

            for (int i = 0; i < members.size(); i++) {
                User user = members.get(i);

                if (user != null && id.equals(user.getId())) {
                    user.setLastOnline(lastOnline);
                    notifyItemChangedSafe(i);
                    return;
                }
            }
        }

        void setSearchingChatId(UUID id) {
            this.searchingChatId = id;
            notifyDataSetChangedSafe();
        }

        List<UUID> getMemberIds() {
            List<UUID> ids = new ArrayList<>();

            for (User user : members) {
                if (user != null && user.getId() != null) {
                    ids.add(user.getId());
                }
            }

            return ids;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < members.size()) {
                return TYPE_MEMBER;
            }

            if (canAddMembers && searchingChatId != null && position == members.size()) {
                return TYPE_SEARCH_CANCEL;
            }

            return TYPE_ADD_BUTTON;
        }

        @Override
        public int getItemCount() {
            int base = members.size();

            if (!canAddMembers) {
                return base;
            }

            if (searchingChatId != null) {
                return base + 2;
            }

            return base + 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MemberViewHolder) {
                bindMember((MemberViewHolder) holder, position);
                return;
            }

            if (holder instanceof AddMemberViewHolder) {
                holder.itemView.setOnClickListener(v ->
                        UiAnimations.animateListItemClick(v, () -> {
                            if (fragment != null && fragment.isAdded()) {
                                fragment.onAddMemberClick();
                            }
                        })
                );
                return;
            }

            if (holder instanceof SearchCancelViewHolder) {
                ((SearchCancelViewHolder) holder).bind(searchingChatId, fragment);
            }
        }

        private void bindMember(MemberViewHolder holder, int position) {
            if (position < 0 || position >= members.size()) {
                return;
            }

            User user = members.get(position);

            if (user == null) {
                return;
            }

            holder.bind(user);

            holder.itemView.setOnClickListener(v ->
                    UiAnimations.animateListItemClick(v, () -> {
                        if (fragment != null && fragment.isAdded()) {
                            fragment.onMemberClick(user);
                        }
                    })
            );

            bindRemoveButton(holder, user);
        }

        private void bindRemoveButton(MemberViewHolder holder, User user) {
            holder.removeButton.animate().cancel();
            holder.removeButton.setScaleX(1f);
            holder.removeButton.setScaleY(1f);
            holder.removeButton.setAlpha(1f);
            holder.removeButton.setOnClickListener(null);
            holder.removeButton.setEnabled(true);
            holder.removeButton.setVisibility(View.VISIBLE);

            UUID targetUserId = user.getId();

            if (!canRemoveMembers) {
                hideRemoveButton(holder);
                return;
            }

            if (targetUserId == null) {
                hideRemoveButton(holder);
                return;
            }

            if (currentUserId != null && currentUserId.equals(targetUserId)) {
                hideRemoveButton(holder);
                return;
            }

            boolean targetIsCreator = creatorUserId != null && creatorUserId.equals(targetUserId);

            if (targetIsCreator && !currentUserIsCreator) {
                hideRemoveButton(holder);
                return;
            }

            holder.removeButton.setOnClickListener(v ->
                    UiAnimations.animateRemoveMemberClick(v, () -> {
                        if (fragment != null && fragment.isAdded()) {
                            fragment.onRemoveMemberClick(targetUserId);
                        }
                    })
            );
        }

        private void hideRemoveButton(MemberViewHolder holder) {
            holder.removeButton.animate().cancel();
            holder.removeButton.setEnabled(false);
            holder.removeButton.setAlpha(1f);
            holder.removeButton.setScaleX(1f);
            holder.removeButton.setScaleY(1f);
            holder.removeButton.setVisibility(View.INVISIBLE);
            holder.removeButton.setOnClickListener(null);
        }

        private void notifyDataSetChangedSafe() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                notifyDataSetChanged();
                return;
            }

            if (recyclerView != null) {
                recyclerView.post(this::notifyDataSetChanged);
            }
        }

        private void notifyItemChangedSafe(int position) {
            if (position < 0) return;

            if (Looper.myLooper() == Looper.getMainLooper()) {
                notifyItemChanged(position);
                return;
            }

            if (recyclerView != null) {
                recyclerView.post(() -> notifyItemChanged(position));
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
                itemView.setAlpha(1f);

                if (user == null || user.getUserData() == null) {
                    name.setText("");
                    details.setText("");
                    onlineStatus.setVisibility(View.GONE);
                    return;
                }

                name.setText(user.getUserData().getName());

                details.setText(itemView.getContext().getString(
                        R.string.member_details,
                        user.getUserData().getGender(),
                        user.getUserData().getAge()
                ));

                onlineStatus.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);
                UiAnimations.animateOnlineStatusAppear(onlineStatus);
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
                itemView.setAlpha(1f);

                if (searchingChatId == null || NEW_GROUP_SEARCH.equals(searchingChatId)) {
                    text.setText(R.string.searching_new_group);
                } else {
                    text.setText(R.string.searching_chat_user);
                }

                cancel.setOnClickListener(v ->
                        UiAnimations.animateSmallButtonClick(v, () -> {
                            if (fragment != null && fragment.isAdded()) {
                                fragment.onCancelSearchClick();
                            }
                        })
                );
            }
        }
    }
}
