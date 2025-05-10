package com.example.aichat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.controller.main.chatlist.ChatController;
import com.example.aichat.model.connection.ConnectionManager;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.OnConnectionEvents;
import com.example.aichat.model.entities.Command;
import com.example.aichat.model.entities.ConnectionInfo;
import com.example.aichat.view.BaseActivity;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DevicesActivity extends BaseActivity {

    private static final int REQUEST_CODE = 783;
    private RecyclerView devicesRecyclerView;
    private DevicesAdapter devicesAdapter;
    private static int currentConnectionId;
    private List<ConnectionInfo> devicesList = new ArrayList<>();
    Button terminateButton;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        connectionManager = ConnectionSingleton.getInstance().getConnectionManager();
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        devicesAdapter = new DevicesAdapter(this::terminateSession);
        devicesRecyclerView.setAdapter(devicesAdapter);
        findViewById(R.id.qrCodeButton).setOnClickListener(v->{
            Intent intent = new Intent(this, QRCodeActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
        });
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        connectionManager.addConnectionEvent(new OnConnectionEvents() {
            @Override
            public void OnCommandGot(Command command) {
                switch (command.getOperation()) {
                    case "GetDevices":
                        ConnectionInfo[] connectionInfos = command.getData("devices", ConnectionInfo[].class);
                        devicesList.addAll(Arrays.asList(connectionInfos));
                        currentConnectionId = command.getData("currentConnectionId", int.class);
                        updateTabs(tabLayout.getSelectedTabPosition());
                        break;
                    case "ConnectionsChange":
                        ConnectionInfo connectionInfo = command.getData("connectionInfo", ConnectionInfo.class);
                        boolean isNewDevice = true;
                        for(int i =0; i<devicesList.size();i++){
                            if(devicesList.get(i).getId()==connectionInfo.getId()){
                                devicesList.set(i,connectionInfo);
                                isNewDevice =  false;
                                break;
                            }
                        }
                        if(isNewDevice){
                            devicesList.add(connectionInfo);
                        }
                        updateTabs(tabLayout.getSelectedTabPosition());
                        break;
                    case "DeleteConnection":
                        ConnectionInfo info = command.getData("connectionInfo", ConnectionInfo.class);
                        for (int i = 0; i < devicesList.size(); i++) {
                            if(devicesList.get(i).equals(info)){
                                devicesList.remove(i);
                                break;
                            }
                        }
                        updateTabs(tabLayout.getSelectedTabPosition());
                        break;
                }
            }

            @Override
            public void OnConnectionFailed() {
                // Handle connection failure
            }

            @Override
            public void OnOpen() {
                // Handle connection open
            }
        });
        terminateButton = findViewById(R.id.terminateButton);
        terminateButton.setOnClickListener(v->{
            terminateSessions(tabLayout.getSelectedTabPosition());
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                updateTerminateButton(position);
                updateTabs(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        connectionManager.SendCommand(new Command("GetDevices"));
    }
    private void updateTerminateButton(int position){
        if(position==3){
            terminateButton.setVisibility(View.GONE);
        }
        else{
            terminateButton.setVisibility(View.VISIBLE);
        }
    }
    private void terminateSessions(int position){
        switch (position) {
            case 3:
            case 0:
                for (int i = 0; i < devicesList.size(); i++) {
                    ConnectionInfo device = devicesList.get(i);
                    if(device.getId()!=currentConnectionId)terminateSession(device.getId());
                }
                break;
            case 1:
                for (int i = 0; i < devicesList.size(); i++) {
                    ConnectionInfo device = devicesList.get(i);
                    if(device.getLastOnlineFormat()!=null)terminateSession(device.getId());
                }
                break;
            case 2:
                for (int i = 0; i < devicesList.size(); i++) {
                    ConnectionInfo device = devicesList.get(i);
                    if(device.getId()!=currentConnectionId&&device.getLastOnlineFormat()==null)terminateSession(device.getId());
                }
                break;
        }
    }
    private void terminateSession(int connectionId){
        Command logoutCommand = new Command("DeleteConnection");
        logoutCommand.addData("connectionId", connectionId);
        connectionManager.SendCommand(logoutCommand);
    }
    private void updateTabs(int position) {
        switch (position) {
            case 0:
                handleYouTabSelected();
                break;
            case 1:
                handleOnlineTabSelected();
                break;
            case 2:
                handleOtherDevicesTabSelected();
                break;
            case 3:
                runOnUiThread(()->  devicesAdapter.updateDevices(devicesList));
                break;
        }
    }

    private void handleYouTabSelected() {
        for(int i=0;i<devicesList.size();i++){
            ConnectionInfo device = devicesList.get(i);
            if(device.getId()== currentConnectionId){
                List<ConnectionInfo> newDevices = new ArrayList<>();
                newDevices.add(device);
                runOnUiThread(()->
                        devicesAdapter.updateDevices(newDevices)
                );
                break;
            }
        }
    }

    private void handleOnlineTabSelected() {
        List<ConnectionInfo> newDevices = new ArrayList<>();
        for(int i=0;i<devicesList.size();i++){
            ConnectionInfo device = devicesList.get(i);
            if(device.getLastOnlineFormat()==null){
                newDevices.add(device);
            }
        }
        runOnUiThread(()-> devicesAdapter.updateDevices(newDevices));
    }

    private void handleOtherDevicesTabSelected() {
        List<ConnectionInfo> newDevices = new ArrayList<>();
        for(int i=0;i<devicesList.size();i++){
            ConnectionInfo device = devicesList.get(i);
            if(device.getLastOnlineFormat()!=null){
                newDevices.add(device);
            }
        }
        runOnUiThread(()-> devicesAdapter.updateDevices(newDevices));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            String resultData = data.getStringExtra("QRCodeResult");
            Command command = new Command("EntryTokenRead");
            command.addData("token", resultData);
            connectionManager.SendCommand(command);
        }
    }

    private static class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

        private List<ConnectionInfo> devices;
        private final OnLogoutClickListener logoutClickListener;

        public interface OnLogoutClickListener {
            void onLogoutClick(int connectionId);
        }

        public DevicesAdapter(OnLogoutClickListener logoutClickListener) {
            this.devices = new ArrayList<>();
            this.logoutClickListener = logoutClickListener;
        }

        public void updateDevices(List<ConnectionInfo> devices) {
            this.devices = devices;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device, parent, false);
            return new DeviceViewHolder(view, logoutClickListener);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            ConnectionInfo device = devices.get(position);
            holder.bind(device);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            private final TextView deviceName;
            private final TextView lastActivity;
            private final ImageButton btn_logout;
            private final OnLogoutClickListener logoutClickListener;

            public DeviceViewHolder(@NonNull View itemView, OnLogoutClickListener logoutClickListener) {
                super(itemView);
                this.logoutClickListener = logoutClickListener;
                deviceName = itemView.findViewById(R.id.deviceName);
                lastActivity = itemView.findViewById(R.id.lastActivity);
                btn_logout = itemView.findViewById(R.id.btn_logout);
            }

            public void bind(ConnectionInfo device) {
                deviceName.setText(device.getDevice());
                lastActivity.setText(device.getLastOnline() == null ?
                        itemView.getContext().getString(R.string.online) :
                        ChatController.getFormattedTime(device.getLastOnlineFormat()));

                if(currentConnectionId == device.getId()) {
                    btn_logout.setVisibility(View.GONE);
                } else {
                    btn_logout.setVisibility(View.VISIBLE);
                    btn_logout.setOnClickListener(v -> {
                        if (logoutClickListener != null) {
                            logoutClickListener.onLogoutClick(device.getId());
                        }
                    });
                }
            }
        }
    }
}