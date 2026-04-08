package com.example.aichat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.dto.request.DeleteConnectionRequest;
import com.example.aichat.dto.request.EntryTokenRequest;
import com.example.aichat.dto.response.ConnectionChangeResponse;
import com.example.aichat.dto.response.DeleteConnectionResponse;
import com.example.aichat.dto.response.DeviceResponse;
import com.example.aichat.dto.response.EntryTokenResponse;
import com.example.aichat.model.QRCodeGenerator;
import com.example.aichat.model.SecurePreferencesManager;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.ConnectionInfo;
import com.example.aichat.model.entities.WSSCommand;
import com.example.aichat.view.BaseActivity;
import com.example.aichat.view.FullScreenHelper;
import com.example.aichat.view.main.MainActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DevicesActivity extends BaseActivity {

    private RecyclerView devicesRecyclerView;
    private DevicesAdapter devicesAdapter;
    private static UUID currentConnectionId;
    private final List<ConnectionInfo> devicesList = new ArrayList<>();
    private View terminateButton;
    private View showMyQRCodeButton;
    private ConnectionManager connectionManager;

    private ActivityResultLauncher<Intent> qrLauncher;

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

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        if (connectionManager == null) {
            String token = SecurePreferencesManager.getAuthToken(this);
            connectionManager = new ConnectionManager(token);
            ConnectionSingleton.getInstance().setConnectionManager(connectionManager);
        }

        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesAdapter = new DevicesAdapter(connectionId -> terminateSession(connectionId));
        devicesRecyclerView.setAdapter(devicesAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        showMyQRCodeButton = findViewById(R.id.showMyQRCodeButton);
        showMyQRCodeButton.setOnClickListener(v -> showQRCodeBottomSheet());

        TabLayout tabLayout = findViewById(R.id.tabLayout);

        qrLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String qr = result.getData().getStringExtra("QRCodeResult");
                        if (qr != null) {
                            connectionManager.SendCommand(new WSSCommand("EntryTokenRead", new EntryTokenRequest(qr)));
                        }
                    }
                }
        );

        findViewById(R.id.qrCodeButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCodeActivity.class);
            qrLauncher.launch(intent);
        });

        connectionManager.addConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(WSSCommand command) {
                switch (command.getOperation()) {
                    case "GetDevices":
                        DeviceResponse deviceResponse = command.getData(DeviceResponse.class);
                        devicesList.clear();
                        devicesList.addAll(Arrays.asList(deviceResponse.connectionInfo));
                        currentConnectionId = deviceResponse.currentConnection;
                        updateTabs(tabLayout.getSelectedTabPosition());
                        break;

                    case "ConnectionsChange":
                        ConnectionChangeResponse changeResponse = command.getData(ConnectionChangeResponse.class);
                        ConnectionInfo info = changeResponse.connectionInfo;

                        boolean isNew = true;
                        for (int i = 0; i < devicesList.size(); i++) {
                            if (devicesList.get(i).getId().equals(info.getId())) {
                                devicesList.set(i, info);
                                isNew = false;
                                break;
                            }
                        }
                        if (isNew) devicesList.add(info);

                        updateTabs(tabLayout.getSelectedTabPosition());
                        break;

                    case "DeleteConnection":
                        DeleteConnectionResponse delResp = command.getData(DeleteConnectionResponse.class);
                        devicesList.removeIf(device -> device.equals(delResp.connectionInfo));
                        updateTabs(tabLayout.getSelectedTabPosition());
                        break;

                    case "EntryTokenRead":
                        handleEntryTokenRead(command);
                        break;

                    case "EntryTokenLoginSuccess":
                        runOnUiThread(() -> {
                            startActivity(new Intent(DevicesActivity.this, MainActivity.class));
                            finish();
                        });
                        break;

                    case "EntryTokenLoginFailed":
                        runOnUiThread(() -> Toast.makeText(
                                DevicesActivity.this,
                                "Не удалось войти по QR-коду",
                                Toast.LENGTH_SHORT
                        ).show());
                        break;
                }
            }

            @Override public void OnConnectionFailed() {}
            @Override public void OnOpen() {}
        });

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

        connectionManager.SendCommand(new WSSCommand("GetDevices"));
    }

    private void handleEntryTokenRead(WSSCommand command) {
        EntryTokenResponse response = command.getData(EntryTokenResponse.class);
        if (response != null && response.token != null) {
            SecurePreferencesManager.saveAuthToken(this, response.token);
            connectionManager.SendCommand(new WSSCommand("EntryTokenRead", new EntryTokenRequest(response.token)));
        }
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
        connectionManager.SendCommand(new WSSCommand("DeleteConnection", new DeleteConnectionRequest(connectionId)));
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
        if (currentConnectionId == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_qr, null);
        bottomSheetDialog.setContentView(view);

        ImageView qrCodeImage = view.findViewById(R.id.qrCodeImageBottomSheet);
        Bitmap qrBitmap = QRCodeGenerator.generateQRCodeImage(currentConnectionId.toString(), 1200, 1200);
        qrCodeImage.setImageBitmap(qrBitmap);
        qrCodeImage.setAdjustViewBounds(true);
        qrCodeImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        ImageButton btnClose = view.findViewById(R.id.btn_close_qr);
        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        View parent = (View) view.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.6);
        parent.getLayoutParams().height = height;
        parent.requestLayout();

        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
        behavior.setPeekHeight(0);

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
