/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bluetooth.communicatorexample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.bluetooth.communicatorexample.fragments.ConversationFragment;
import com.bluetooth.communicatorexample.fragments.PairingFragment;
import com.bluetooth.communicatorexample.tools.Tools;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int PAIRING_FRAGMENT = 0;
    public static final int CONVERSATION_FRAGMENT = 1;
    public static final int DEFAULT_FRAGMENT = PAIRING_FRAGMENT;
    public static final int NO_PERMISSIONS = -10;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private Global global;
    private int currentFragment = -1;
    private ArrayList<Callback> clientsCallbacks = new ArrayList<>();
    private CoordinatorLayout fragmentContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        global = (Global) getApplication();

        // Clean fragments (only if the app is recreated (When user disable permission))
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // Remove previous fragments (case of the app was restarted after changed permission on android 6 and higher)
        List<Fragment> fragmentList = fragmentManager.getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        fragmentContainer = findViewById(R.id.fragment_container);

        global.getBluetoothCommunicator().addCallback(new BluetoothCommunicator.Callback() {
            @Override
            public void onAdvertiseStarted() {
                super.onAdvertiseStarted();
                if (global.getBluetoothCommunicator().isDiscovering()) {
                    notifySearchStarted();
                }
            }

            @Override
            public void onDiscoveryStarted() {
                super.onDiscoveryStarted();
                if (global.getBluetoothCommunicator().isAdvertising()) {
                    notifySearchStarted();
                }
            }

            @Override
            public void onAdvertiseStopped() {
                super.onAdvertiseStopped();
                if (!global.getBluetoothCommunicator().isDiscovering()) {
                    notifySearchStopped();
                }
            }

            @Override
            public void onDiscoveryStopped() {
                super.onDiscoveryStopped();
                if (!global.getBluetoothCommunicator().isAdvertising()) {
                    notifySearchStopped();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // when we return to the app's gui we choose which fragment to start based on connection status
        if (global.getBluetoothCommunicator().getConnectedPeersList().size() == 0) {
            setFragment(DEFAULT_FRAGMENT);
        } else {
            setFragment(CONVERSATION_FRAGMENT);
        }
    }

    public void setFragment(int fragmentName) {
        switch (fragmentName) {
            case PAIRING_FRAGMENT: {
                // possible setting of the fragment
                if (getCurrentFragment() != PAIRING_FRAGMENT) {
                    PairingFragment paringFragment = new PairingFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    transaction.replace(R.id.fragment_container, paringFragment);
                    transaction.commit();
                    currentFragment = PAIRING_FRAGMENT;
                }
                break;
            }
            case CONVERSATION_FRAGMENT: {
                // possible setting of the fragment
                if (getCurrentFragment() != CONVERSATION_FRAGMENT) {
                    ConversationFragment conversationFragment = new ConversationFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, conversationFragment);
                    transaction.commit();
                    currentFragment = CONVERSATION_FRAGMENT;
                }
                break;
            }
        }
    }

    public int getCurrentFragment() {
        if (currentFragment != -1) {
            return currentFragment;
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                if (currentFragment.getClass().equals(PairingFragment.class)) {
                    return PAIRING_FRAGMENT;
                }
                if (currentFragment.getClass().equals(ConversationFragment.class)) {
                    return CONVERSATION_FRAGMENT;
                }
            }
        }
        return -1;
    }

    @Override
    public void onBackPressed() {
        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitFromConversation();
            }
        };
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            if (fragment instanceof ConversationFragment) {
                showConfirmExitDialog(confirmExitListener);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void exitFromConversation() {
        if (global.getBluetoothCommunicator().getConnectedPeersList().size() > 0) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof ConversationFragment) {
                ConversationFragment conversationFragment = (ConversationFragment) fragment;
                conversationFragment.appearLoading();
            }
            global.getBluetoothCommunicator().disconnectFromAll();
        } else {
            setFragment(DEFAULT_FRAGMENT);
        }
    }

    protected void showConfirmExitDialog(DialogInterface.OnClickListener confirmListener) {
        //creazione del dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage("Confirm exit");
        builder.setPositiveButton(android.R.string.ok, confirmListener);
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public int startSearch() {
        if (global.getBluetoothCommunicator().isBluetoothLeSupported() == BluetoothCommunicator.SUCCESS) {
            if (Tools.hasPermissions(this, REQUIRED_PERMISSIONS)) {
                int advertisingCode = global.getBluetoothCommunicator().startAdvertising();
                int discoveringCode = global.getBluetoothCommunicator().startDiscovery();
                if (advertisingCode == discoveringCode) {
                    return advertisingCode;
                }
                if (advertisingCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED || discoveringCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED) {
                    return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
                }
                if (advertisingCode == BluetoothCommunicator.SUCCESS || discoveringCode == BluetoothCommunicator.SUCCESS) {
                    if (advertisingCode == BluetoothCommunicator.ALREADY_STARTED || discoveringCode == BluetoothCommunicator.ALREADY_STARTED) {
                        return BluetoothCommunicator.SUCCESS;
                    }
                }
                return BluetoothCommunicator.ERROR;
            } else {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
                return NO_PERMISSIONS;
            }
        } else {
            return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
        }
    }

    public int stopSearch(boolean tryRestoreBluetoothStatus) {
        int advertisingCode = global.getBluetoothCommunicator().stopAdvertising(tryRestoreBluetoothStatus);
        int discoveringCode = global.getBluetoothCommunicator().stopDiscovery(tryRestoreBluetoothStatus);
        if (advertisingCode == discoveringCode) {
            return advertisingCode;
        }
        if (advertisingCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED || discoveringCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED) {
            return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
        }
        if (advertisingCode == BluetoothCommunicator.SUCCESS || discoveringCode == BluetoothCommunicator.SUCCESS) {
            if (advertisingCode == BluetoothCommunicator.ALREADY_STOPPED || discoveringCode == BluetoothCommunicator.ALREADY_STOPPED) {
                return BluetoothCommunicator.SUCCESS;
            }
        }
        return BluetoothCommunicator.ERROR;
    }

    public boolean isSearching() {
        return global.getBluetoothCommunicator().isAdvertising() && global.getBluetoothCommunicator().isDiscovering();
    }

    public void connect(Peer peer) {
        stopSearch(false);
        global.getBluetoothCommunicator().connect(peer);
    }

    public void acceptConnection(Peer peer) {
        global.getBluetoothCommunicator().acceptConnection(peer);
    }

    public void rejectConnection(Peer peer) {
        global.getBluetoothCommunicator().rejectConnection(peer);
    }

    public int disconnect(Peer peer) {
        return global.getBluetoothCommunicator().disconnect(peer);
    }

    public CoordinatorLayout getFragmentContainer() {
        return fragmentContainer;
    }



    public void addCallback(Callback callback) {
        // in this way the listener will listen to both this activity and the communicatorexample
        global.getBluetoothCommunicator().addCallback(callback);
        clientsCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        global.getBluetoothCommunicator().removeCallback(callback);
        clientsCallbacks.remove(callback);
    }

    private void notifyMissingSearchPermission() {
        for (int i = 0; i < clientsCallbacks.size(); i++) {
            clientsCallbacks.get(i).onMissingSearchPermission();
        }
    }

    private void notifySearchPermissionGranted() {
        for (int i = 0; i < clientsCallbacks.size(); i++) {
            clientsCallbacks.get(i).onSearchPermissionGranted();
        }
    }

    private void notifySearchStarted() {
        for (int i = 0; i < clientsCallbacks.size(); i++) {
            clientsCallbacks.get(i).onSearchStarted();
        }
    }

    private void notifySearchStopped() {
        for (int i = 0; i < clientsCallbacks.size(); i++) {
            clientsCallbacks.get(i).onSearchStopped();
        }
    }

    public static class Callback extends BluetoothCommunicator.Callback {
        public void onSearchStarted() {
        }

        public void onSearchStopped() {
        }

        public void onMissingSearchPermission() {
        }

        public void onSearchPermissionGranted() {
        }
    }
}