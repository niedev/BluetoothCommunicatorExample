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

package com.bluetooth.communicatorexample.fragments;

import android.animation.Animator;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bluetooth.communicator.tools.Timer;
import com.bluetooth.communicatorexample.Global;
import com.bluetooth.communicatorexample.MainActivity;
import com.bluetooth.communicatorexample.R;
import com.bluetooth.communicatorexample.gui.ButtonSearch;
import com.bluetooth.communicatorexample.gui.CustomAnimator;
import com.bluetooth.communicatorexample.gui.GuiTools;
import com.bluetooth.communicatorexample.gui.PeerListAdapter;
import com.bluetooth.communicatorexample.gui.RequestDialog;
import com.bluetooth.communicatorexample.tools.Tools;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


public class PairingFragment extends Fragment {
    public static final int CONNECTION_TIMEOUT = 5000;
    private RequestDialog connectionRequestDialog;
    private RequestDialog connectionConfirmDialog;
    private ConstraintLayout constraintLayout;
    private Peer confirmConnectionPeer;
    private ListView listViewGui;
    private Timer connectionTimer;
    @Nullable
    private PeerListAdapter listView;
    private TextView discoveryDescription;
    private TextView noDevices;
    private TextView noPermissions;
    private TextView noBluetoothLe;
    private final Object lock = new Object();
    private MainActivity.Callback communicatorCallback;
    private CustomAnimator animator = new CustomAnimator();
    private Peer connectingPeer;
    protected Global global;
    protected MainActivity activity;
    private static final float LOADING_SIZE_DP = 24;
    protected boolean isLoadingVisible = false;
    private boolean appearSearchButton = false;
    protected boolean isLoadingAnimating;  // animation appearance or disappearance of the loading
    protected ButtonSearch buttonSearch;
    private ProgressBar loading;
    private ArrayList<CustomAnimator.EndListener> listeners = new ArrayList<>();

    public PairingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        communicatorCallback = new MainActivity.Callback() {
            @Override
            public void onSearchStarted() {
                buttonSearch.setSearching(true, animator);
            }

            @Override
            public void onSearchStopped() {
                buttonSearch.setSearching(false, animator);
            }

            @Override
            public void onConnectionRequest(final Peer peer) {
                super.onConnectionRequest(peer);
                if (peer != null) {
                    String time = DateFormat.getDateTimeInstance().format(new Date());
                    connectionRequestDialog = new RequestDialog(activity, "Accept connection request from " + peer.getName() + " ?", 15000, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.acceptConnection(peer);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.rejectConnection(peer);
                        }
                    });
                    connectionRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            connectionRequestDialog = null;
                        }
                    });
                    connectionRequestDialog.show();
                }
            }

            @Override
            public void onConnectionSuccess(Peer peer, int source) {
                super.onConnectionSuccess(peer, source);
                connectingPeer = null;
                resetConnectionTimer();
                activity.setFragment(MainActivity.CONVERSATION_FRAGMENT);
            }

            @Override
            public void onConnectionFailed(Peer peer, int errorCode) {
                super.onConnectionFailed(peer, errorCode);
                if (connectingPeer != null) {
                    if (connectionTimer != null && !connectionTimer.isFinished() && errorCode != BluetoothCommunicator.CONNECTION_REJECTED) {
                        // the timer has not expired and the connection has not been refused, so we try again
                        activity.connect(peer);
                    } else {
                        // the timer has expired, so the failure is notified
                        clearFoundPeers();
                        startSearch();
                        activateInputs();
                        disappearLoading(true, null);
                        connectingPeer = null;
                        if (errorCode == BluetoothCommunicator.CONNECTION_REJECTED) {
                            Toast.makeText(activity, peer.getName() + " refused the connection request", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "Connection error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onPeerFound(Peer peer) {
                super.onPeerFound(peer);
                synchronized (lock) {
                    if (listView != null) {
                        BluetoothAdapter bluetoothAdapter = global.getBluetoothCommunicator().getBluetoothAdapter();
                        int index = listView.indexOfPeer(peer.getUniqueName());
                        if (index == -1) {
                            listView.add(peer);
                        } else {
                            Peer peer1 = listView.get(index);
                            if (peer.isBonded(bluetoothAdapter)) {
                                listView.set(index, peer);
                            } else if (peer1.isBonded(bluetoothAdapter)) {
                                listView.set(index, listView.get(index));
                            } else {
                                listView.set(index, peer);
                            }
                        }
                    }
                }
            }

            @Override
            public void onPeerUpdated(Peer peer, Peer newPeer) {
                super.onPeerUpdated(peer, newPeer);
                onPeerFound(newPeer);
            }

            @Override
            public void onPeerLost(Peer peer) {
               synchronized (lock) {
                    if (listView != null) {
                        listView.remove(peer);
                        if (peer.equals(getConfirmConnectionPeer())) {
                            RequestDialog requestDialog = getConnectionConfirmDialog();
                            if (requestDialog != null) {
                                requestDialog.cancel();
                            }
                        }
                    }
                }
            }

            @Override
            public void onBluetoothLeNotSupported() {

            }

            @Override
            public void onMissingSearchPermission() {
                super.onMissingSearchPermission();
                clearFoundPeers();
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    // appearance of the written of missing permission
                    listViewGui.setVisibility(View.GONE);
                    noDevices.setVisibility(View.GONE);
                    discoveryDescription.setVisibility(View.GONE);
                    noPermissions.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSearchPermissionGranted() {
                super.onSearchPermissionGranted();
                if (noPermissions.getVisibility() == View.VISIBLE) {
                    // disappearance of the written of missing permission
                    noPermissions.setVisibility(View.GONE);
                    noDevices.setVisibility(View.VISIBLE);
                    discoveryDescription.setVisibility(View.VISIBLE);
                    initializePeerList();
                } else {
                    //reset list view
                    clearFoundPeers();
                }
                startSearch();
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pairing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.container);
        listViewGui = view.findViewById(R.id.list_view);
        discoveryDescription = view.findViewById(R.id.discoveryDescription);
        noDevices = view.findViewById(R.id.noDevices);
        noPermissions = view.findViewById(R.id.noPermission);
        noBluetoothLe = view.findViewById(R.id.noBluetoothLe);
        buttonSearch = view.findViewById(R.id.searchButton);
        loading = view.findViewById(R.id.progressBar2);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) requireActivity();
        global = (Global) activity.getApplication();
        Toolbar toolbar = activity.findViewById(R.id.toolbarPairing);
        activity.setActionBar(toolbar);
        // we give the constraint layout the information on the system measures (status bar etc.), which has the fragmentContainer,
        // because they are not passed to it if started with a Transaction and therefore it overlaps the status bar because it fitsSystemWindows does not work
        WindowInsets windowInsets = activity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {
            constraintLayout.dispatchApplyWindowInsets(windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), 0));
        }

        // setting of array adapter
        initializePeerList();
        listViewGui.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                synchronized (lock) {
                    if (listView != null) {
                        // start the pop up and then connect to the peer
                        if (listView.isClickable()) {
                            Peer item = listView.get(i);
                            connect(item);
                        } else {
                            listView.getCallback().onClickNotAllowed(listView.getShowToast());
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // release buttons and eliminate any loading
        activateInputs();
        disappearLoading(true, null);
        // if you don't have permission to search, activate from here
        if (!Tools.hasPermissions(activity, MainActivity.REQUIRED_PERMISSIONS)) {
            startSearch();
        }

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.isSearching()) {
                    activity.stopSearch(false);
                    clearFoundPeers();
                } else {
                    startSearch();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        clearFoundPeers();

        activity.addCallback(communicatorCallback);
        // if you have permission to search it is activated from here
        if (Tools.hasPermissions(activity, MainActivity.REQUIRED_PERMISSIONS)) {
            startSearch();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activity.removeCallback(communicatorCallback);
        stopSearch();
        //communicatorCallback.onSearchStopped();
        if (connectingPeer != null) {
            activity.disconnect(connectingPeer);
            connectingPeer = null;
        }
    }

    private void connect(final Peer peer) {
        connectingPeer = peer;
        confirmConnectionPeer = peer;
        connectionConfirmDialog = new RequestDialog(activity, "Are you sure to connect with " + peer.getName() + "?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deactivateInputs();
                appearLoading(null);
                activity.connect(peer);
                startConnectionTimer();
            }
        }, null);
        connectionConfirmDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                confirmConnectionPeer = null;
                connectionConfirmDialog = null;
            }
        });
        connectionConfirmDialog.show();
    }

    protected void startSearch() {
        int result = activity.startSearch();
        if (result != BluetoothCommunicator.SUCCESS) {
            if (result == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED && noBluetoothLe.getVisibility() != View.VISIBLE) {
                // appearance of the bluetooth le missing sign
                listViewGui.setVisibility(View.GONE);
                noDevices.setVisibility(View.GONE);
                discoveryDescription.setVisibility(View.GONE);
                noBluetoothLe.setVisibility(View.VISIBLE);
            } else if (result != MainActivity.NO_PERMISSIONS && result != BluetoothCommunicator.ALREADY_STARTED) {
                Toast.makeText(activity, "Error starting search", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopSearch() {
        activity.stopSearch(connectingPeer == null);
    }

    private void activateInputs() {
        //click reactivation of listView
        setListViewClickable(true, true);
    }

    private void deactivateInputs() {
        //click deactivation of listView
        setListViewClickable(false, true);
    }

    public Peer getConfirmConnectionPeer() {
        return confirmConnectionPeer;
    }

    public RequestDialog getConnectionConfirmDialog() {
        return connectionConfirmDialog;
    }

    private void startConnectionTimer() {
        connectionTimer = new Timer(CONNECTION_TIMEOUT);
        connectionTimer.start();
    }

    private void resetConnectionTimer() {
        if (connectionTimer != null) {
            connectionTimer.cancel();
            connectionTimer = null;
        }
    }

    private void initializePeerList() {
        final PeerListAdapter.Callback callback = new PeerListAdapter.Callback() {
            @Override
            public void onFirstItemAdded() {
                super.onFirstItemAdded();
                discoveryDescription.setVisibility(View.GONE);
                noDevices.setVisibility(View.GONE);
                listViewGui.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLastItemRemoved() {
                super.onLastItemRemoved();
                listViewGui.setVisibility(View.GONE);
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    discoveryDescription.setVisibility(View.VISIBLE);
                    noDevices.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClickNotAllowed(boolean showToast) {
                super.onClickNotAllowed(showToast);
                Toast.makeText(activity, "Cannot interact with devices during connection", Toast.LENGTH_SHORT).show();
            }
        };

        listView = new PeerListAdapter(activity, new ArrayList<Peer>(), callback);
        listViewGui.setAdapter(listView);
    }

    public void clearFoundPeers() {
        if (listView != null) {
            listView.clear();
        }
    }

    public void setListViewClickable(boolean isClickable, boolean showToast) {
        if (listView != null) {
            listView.setClickable(isClickable, showToast);
        }
    }


    /**
     * In this method we not only make the loading appear but first we make the ButtonSearch disappear,
     * these two animations will be considered the loading appearance animation
     **/
    public void appearLoading(@Nullable CustomAnimator.EndListener responseListener) {
        if (responseListener != null) {
            listeners.add(responseListener);
        }
        isLoadingVisible = true;
        if (!isLoadingAnimating) {
            if (loading.getVisibility() != View.VISIBLE) {  // if the object has not already appeared graphically
                //animation execution
                isLoadingAnimating = true;
                buttonSearch.setVisible(false, new CustomAnimator.EndListener() {
                    @Override
                    public void onAnimationEnd() {
                        int loadingSizePx = GuiTools.convertDpToPixels(activity, LOADING_SIZE_DP);
                        Animator animation = animator.createAnimatorSize(loading, 1, 1, loadingSizePx, loadingSizePx, getResources().getInteger(R.integer.durationShort));
                        animation.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                loading.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                isLoadingAnimating = false;
                                if (!isLoadingVisible) {   // if isLoadingVisible has changed in the meantime
                                    disappearLoading(appearSearchButton, null);
                                } else {
                                    notifyLoadingAnimationEnd();
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        animation.start();

                    }
                });
            } else {
                notifyLoadingAnimationEnd();
            }
        }
    }

    /**
     * In this method not only do we make the loading disappear but, if set by the user, after that we also make the ButtonSearch reappear,
     * these two animations will be considered the disappearance animation of the loading
     **/
    public void disappearLoading(final boolean appearSearchButton, @Nullable CustomAnimator.EndListener responseListener) {
        if (responseListener != null) {
            listeners.add(responseListener);
        }
        this.isLoadingVisible = false;
        this.appearSearchButton = appearSearchButton;
        if (!isLoadingAnimating) {
            if (loading.getVisibility() != View.GONE) {  // if the object has not already disappeared graphically
                // animation execution
                isLoadingAnimating = true;
                int loadingSizePx = GuiTools.convertDpToPixels(activity, LOADING_SIZE_DP);
                Animator animation = animator.createAnimatorSize(loading, loadingSizePx, loadingSizePx, 1, 1, getResources().getInteger(R.integer.durationShort));
                animation.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loading.setVisibility(View.GONE);
                        CustomAnimator.EndListener listener = new CustomAnimator.EndListener() {
                            @Override
                            public void onAnimationEnd() {
                                isLoadingAnimating = false;
                                if (isLoadingVisible) {   // if isLoadingVisible has changed in the meantime
                                    appearLoading(null);
                                } else {
                                    notifyLoadingAnimationEnd();
                                }
                            }
                        };
                        if (appearSearchButton) {
                            buttonSearch.setVisible(true, listener);
                        } else {
                            listener.onAnimationEnd();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                animation.start();
            } else {
                notifyLoadingAnimationEnd();
            }
        }
    }

    private void notifyLoadingAnimationEnd() {
        // notify finished animation and elimination of listeners
        while (listeners.size() > 0) {
            listeners.remove(0).onAnimationEnd();
        }
    }
}