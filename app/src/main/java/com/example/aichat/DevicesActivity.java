package com.example.aichat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.dto.response.ConnectionChangeResponse;
import com.example.aichat.model.QRCodeGenerator;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.EventHandler;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.SignalRCommand;
import com.example.aichat.model.entities.ConnectionInfo;
import com.example.aichat.model.utils.JsonHelper;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.FullScreenHelper;
import com.example.aichat.view.main.chat.ui.UiAnimations;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DevicesActivity extends BaseActivity {

    private RecyclerView devicesRecyclerView;
    private DevicesAdapter devicesAdapter;
    private static UUID currentConnectionId;
    private List<ConnectionInfo> devicesList;
    private View terminateButton;
    private View showMyQRCodeButton;
    private ConnectionDispatcher dispatcher;
    private TabLayout tabLayout;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        FullScreenHelper.enableFullScreen(getWindow());
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);
        ViewCompat.setOnApplyWindowInsetsListener(devicesRecyclerView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(
                    v.getPaddingLeft(),
                    topInset + 20,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });

        Intent intent = getIntent();

        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
        currentConnectionId = dispatcher.getConnectionId();

        dispatcher.addEventListener("ConnectionChanged", ConnectionChangeResponse.class, new EventHandler<ConnectionChangeResponse>() {
            @Override
            public void handle(SignalRCommand<ConnectionChangeResponse> command) {
                ConnectionChangeResponse response = command.getPayload();
                assert response != null;
                devicesList = response.connections;
                updateTabs(tabLayout.getSelectedTabPosition());
            }
        });




        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesAdapter = new DevicesAdapter(connectionId -> terminateSession(connectionId));
        devicesList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(JsonHelper.Deserialize(intent.getStringExtra("devices"), ConnectionInfo[].class))));
        //devicesAdapter.updateDevices(devicesList);
        updateTabs(0);
        devicesRecyclerView.setAdapter(devicesAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        showMyQRCodeButton = findViewById(R.id.showMyQRCodeButton);
        showMyQRCodeButton.setOnClickListener(v -> showQRCodeBottomSheet());

        tabLayout = findViewById(R.id.tabLayout);
        terminateButton = findViewById(R.id.terminateButton);
        terminateButton.setOnClickListener(v -> terminateSessions(tabLayout.getSelectedTabPosition()));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateTerminateButton(tab.getPosition());
                updateTabs(tab.getPosition());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateTerminateButton(int position) {
        terminateButton.setVisibility(View.VISIBLE);
    }

    private void terminateSessions(int position) {
        switch (position) {
            case 3:
            case 0:
                for (ConnectionInfo device : devicesList)
                    if (!device.getId().equals(currentConnectionId))
                        terminateSession(device.getId());
                break;

            case 1:
                for (ConnectionInfo device : devicesList)
                    if (device.getLastOnlineFormat() != null)
                        terminateSession(device.getId());
                break;

            case 2:
                for (ConnectionInfo device : devicesList)
                    if (!device.getId().equals(currentConnectionId)
                            && device.getLastOnlineFormat() == null)
                        terminateSession(device.getId());
                break;
        }
    }

    private void terminateSession(UUID connectionId) {
        dispatcher.sendHttpRequestAsync("/api/session/"+connectionId, HttpClient.HTTPMethod.DELETE, null, false).thenAccept((cmd)->{
                 if(cmd.isSuccess()){
                     devicesList.removeIf(device -> device.getId().equals(connectionId));
                     updateTabs(tabLayout.getSelectedTabPosition());
                 }
                }
        );
    }

    private void updateTabs(int position) {
        switch (position) {
            case 0: handleYouTabSelected(); break;
            case 1: handleOnlineTabSelected(); break;
            case 2: handleOtherDevicesTabSelected(); break;
            case 3: handleAllDevicesTabSelected(); break;
        }
    }

    private void handleYouTabSelected() {
        List<ConnectionInfo> newDevices = new ArrayList<>();
        for (ConnectionInfo device : devicesList)
            if (device.getId().equals(currentConnectionId)) {
                newDevices.add(device);
                break;
            }
        runOnUiThread(() -> devicesAdapter.updateDevices(newDevices));
    }

    private void handleOnlineTabSelected() {
        List<ConnectionInfo> newDevices = new ArrayList<>();
        for (ConnectionInfo device : devicesList)
            if (device.getLastOnlineFormat() == null)
                newDevices.add(device);

        runOnUiThread(() -> devicesAdapter.updateDevices(newDevices));
    }

    private void handleOtherDevicesTabSelected() {
        List<ConnectionInfo> newDevices = new ArrayList<>();
        for (ConnectionInfo device : devicesList)
            if (device.getLastOnlineFormat() != null)
                newDevices.add(device);

        runOnUiThread(() -> devicesAdapter.updateDevices(newDevices));
    }

    private void handleAllDevicesTabSelected() {
        List<ConnectionInfo> sorted = new ArrayList<>(devicesList);

        sorted.sort((a, b) -> {
            boolean aOnline = a.getLastOnlineFormat() == null;
            boolean bOnline = b.getLastOnlineFormat() == null;

            if (aOnline && !bOnline) return -1;
            if (!aOnline && bOnline) return 1;

            if (!aOnline && !bOnline) {
                return b.getLastOnlineFormat().compareTo(a.getLastOnlineFormat());
            }

            return 0;
        });

        runOnUiThread(() -> devicesAdapter.updateDevices(sorted));
    }

    private void showQRCodeBottomSheet() {
        final boolean[] isCodeUsed = {false};
        final boolean[] isQrReady = {false};
        final boolean[] isFirstTextSet = {false};

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_qr, null);
        bottomSheetDialog.setContentView(view);

        TextView qrCodeTextView = view.findViewById(R.id.qrCodeTextView);
        ImageView qrCodeImage = view.findViewById(R.id.qrCodeImageBottomSheet);
        View blurOverlay = view.findViewById(R.id.qrBlurOverlay);
        ImageView tapHintIcon = view.findViewById(R.id.iv_tap_hint);
        TextView tapHintText = view.findViewById(R.id.tv_tap_hint);

        // Изначально текст пустой и прозрачный
        qrCodeTextView.setText("");
        qrCodeTextView.setAlpha(0f);

        blurOverlay.setVisibility(View.VISIBLE);
        blurOverlay.setAlpha(1f);
        blurOverlay.setClickable(true);
        blurOverlay.setBackgroundColor(0xCC000000);

        qrCodeImage.setAlpha(0f);
        qrCodeImage.setScaleX(0.8f);
        qrCodeImage.setScaleY(0.8f);

        tapHintIcon.setAlpha(0.7f);
        tapHintIcon.setScaleX(1f);
        tapHintIcon.setScaleY(1f);
        tapHintText.setAlpha(0.8f);

        Handler handler = new Handler(Looper.getMainLooper());

        UiAnimations.startInfinitePulseAnimation(tapHintIcon, tapHintText);

        dispatcher.addEventListener("EntryCodeUsed", String.class, command -> {
            isCodeUsed[0] = true;
            runOnUiThread(() -> {
                qrCodeImage.setImageResource(R.drawable.ic_done);
                UiAnimations.animateTextChange(qrCodeTextView, getString(R.string.qr_scanned), 300);
                UiAnimations.stopInfinitePulseAnimation(tapHintIcon, tapHintText);
                handler.postDelayed(() -> {
                    if (bottomSheetDialog.isShowing()) {
                        bottomSheetDialog.dismiss();
                    }
                }, 3000);
            });
        });

        dispatcher.sendHttpRequestAsync("/api/auth/code/generate", HttpClient.HTTPMethod.GET, null, false)
                .thenAccept(cmd -> {
                    if (cmd.isSuccess()) {
                        String code = cmd.getData(String.class);
                        Bitmap qr = QRCodeGenerator.generateQRCodeImage(code, 1200, 1200);
                        runOnUiThread(() -> {
                            qrCodeImage.setImageBitmap(qr);
                            isQrReady[0] = true;
                            // Первый текст появляется с анимацией появления
                            if (!isFirstTextSet[0]) {
                                isFirstTextSet[0] = true;
                                qrCodeTextView.setText(getString(R.string.tap_to_reveal_qr));
                                qrCodeTextView.animate()
                                        .alpha(1f)
                                        .setDuration(300)
                                        .start();
                            } else {
                                UiAnimations.animateTextChange(qrCodeTextView, getString(R.string.tap_to_reveal_qr), 300);
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            UiAnimations.animateTextChange(qrCodeTextView, getString(R.string.qr_generation_failed), 300);
                            blurOverlay.setClickable(false);
                            UiAnimations.stopInfinitePulseAnimation(tapHintIcon, tapHintText);
                        });
                    }
                });

        blurOverlay.setOnClickListener(v -> {
            if (!isQrReady[0]) {
                UiAnimations.animateTextChange(qrCodeTextView, getString(R.string.generating_qr_please_wait), 300);
                return;
            }

            UiAnimations.stopInfinitePulseAnimation(tapHintIcon, tapHintText);

            UiAnimations.animateQrRevealWithScale(blurOverlay, qrCodeImage, tapHintIcon, tapHintText, () -> {
                UiAnimations.animateTextChange(qrCodeTextView, getString(R.string.scan_qr_code), 300);
            });
        });

        view.findViewById(R.id.btn_close_qr).setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setOnDismissListener(dialog -> {
            UiAnimations.stopInfinitePulseAnimation(tapHintIcon, tapHintText);
            if (!isCodeUsed[0]) {
                dispatcher.sendHttpRequestAsync("/api/auth/code", HttpClient.HTTPMethod.DELETE, null, false);
            }
        });

        View parent = (View) view.getParent();
        if (parent != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.6);
            parent.getLayoutParams().height = height;
            parent.requestLayout();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setPeekHeight(0);
        }

        bottomSheetDialog.show();
    }
    private static class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

        private List<ConnectionInfo> devices = new ArrayList<>();
        private final OnLogoutClickListener logoutClickListener;

        public interface OnLogoutClickListener {
            void onLogoutClick(UUID connectionId);
        }

        public DevicesAdapter(OnLogoutClickListener listener) {
            this.logoutClickListener = listener;
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
            return new DeviceViewHolder(view, logoutClickListener);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            holder.bind(devices.get(position));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        public void updateDevices(List<ConnectionInfo> devices) {
            this.devices = devices;
            notifyDataSetChanged();
        }

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            private final TextView deviceName;
            private final TextView lastActivity;
            private final ImageButton btn_logout;
            private final OnLogoutClickListener logoutClickListener;

            public DeviceViewHolder(@NonNull View itemView, OnLogoutClickListener listener) {
                super(itemView);
                deviceName = itemView.findViewById(R.id.deviceName);
                lastActivity = itemView.findViewById(R.id.lastActivity);
                btn_logout = itemView.findViewById(R.id.btn_logout);
                this.logoutClickListener = listener;
            }

            public void bind(ConnectionInfo device) {
                deviceName.setText(device.getDevice());
                lastActivity.setText(
                        device.getLastOnline() == null
                                ? itemView.getContext().getString(R.string.online)
                                : ChatController.getFormattedTime(device.getLastOnlineFormat())
                );

                if (currentConnectionId != null && currentConnectionId.equals(device.getId())) {
                    btn_logout.setVisibility(View.GONE);
                } else {
                    btn_logout.setVisibility(View.VISIBLE);
                    btn_logout.setOnClickListener(v -> logoutClickListener.onLogoutClick(device.getId()));
                }
            }
        }
    }
}
