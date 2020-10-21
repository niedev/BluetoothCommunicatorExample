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

package com.bluetooth.communicator.fragments;


import android.animation.Animator;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bluetooth.communicator.Global;
import com.bluetooth.communicator.MainActivity;
import com.bluetooth.communicator.R;
import com.bluetooth.communicator.gui.CustomAnimator;
import com.bluetooth.communicator.gui.GuiTools;
import com.bluetooth.communicator.gui.MessagesAdapter;
import com.bluetooth.communicatorlibrary.BluetoothCommunicator;
import com.bluetooth.communicatorlibrary.Message;
import com.bluetooth.communicatorlibrary.Peer;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


public class ConversationFragment extends Fragment {
    private ProgressBar loading;
    private static final float LOADING_SIZE_DP = 24;
    private EditText editText;
    private AppCompatImageButton sendButton;
    private RecyclerView mRecyclerView;
    protected TextView description;
    private ConstraintLayout constraintLayout;
    private BluetoothCommunicator.Callback communicatorCallback;
    private Global global;
    private MainActivity activity;
    private MessagesAdapter mAdapter;
    private RecyclerView.SmoothScroller smoothScroller;

    public ConversationFragment() {
        //an empty constructor is always needed for fragments
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        communicatorCallback = new BluetoothCommunicator.Callback() {
            @Override
            public void onConnectionLost(Peer peer) {
                super.onConnectionLost(peer);
                Toast.makeText(activity,"Connection lost, reconnecting...",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectionResumed(Peer peer) {
                super.onConnectionResumed(peer);
                Toast.makeText(activity,"Connection resumed",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onMessageReceived(Message message, int source) {
                super.onMessageReceived(message, source);
                /* means that we have received a message containing TEXT, for know the sender we can call message.getSender() that return
                the peer that have sent the message, we can ignore source, it indicate only if we have received the message
                as clients or as servers
                 */
                mAdapter.addMessage(message);
                //smooth scroll
                smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
            }

            @Override
            public void onDisconnected(Peer peer, int peersLeft) {
                super.onDisconnected(peer, peersLeft);
                /*means that the peer is disconnected, peersLeft indicate the number of connected peers remained
                 */
                if (peersLeft == 0) {
                    activity.setFragment(MainActivity.DEFAULT_FRAGMENT);
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editText = view.findViewById(R.id.editText);
        sendButton = view.findViewById(R.id.button_send);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        description = view.findViewById(R.id.description);
        loading = view.findViewById(R.id.progressBar2);
        constraintLayout = view.findViewById(R.id.container2);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) requireActivity();
        global = (Global) activity.getApplication();
        Toolbar toolbar = activity.findViewById(R.id.toolbarConversation);
        activity.setActionBar(toolbar);
        // we give the constraint layout the information on the system measures (status bar etc.), which has the fragmentContainer,
        // because they are not passed to it if started with a Transaction and therefore it overlaps the status bar because it fitsSystemWindows does not work
        WindowInsets windowInsets = activity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {
            constraintLayout.dispatchApplyWindowInsets(windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), 0));
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layoutManager);
        smoothScroller = new LinearSmoothScroller(activity) {
            @Override
            protected int calculateTimeForScrolling(int dx) {
                return 100;
            }
        };

        mAdapter = new MessagesAdapter(global.getBluetoothCommunicator().getUniqueName(), new MessagesAdapter.Callback() {
            @Override
            public void onFirstItemAdded() {
                description.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (global.getBluetoothCommunicator().getConnectedPeersList().size() > 0) {
                    //sending message
                    if (editText.getText().length() > 0) {
                        //the sender will be inserted by the receiver device, so you don't need to enter it
                        Message message = new Message(global, "m", editText.getText().toString(), global.getBluetoothCommunicator().getConnectedPeersList().get(0));
                        global.getBluetoothCommunicator().sendMessage(message);
                        editText.setText("");
                        //aggiunta del messaggio alla lista dei messaggi
                        mAdapter.addMessage(message);
                        //smooth scroll
                        smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                        mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        global.getBluetoothCommunicator().addCallback(communicatorCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        global.getBluetoothCommunicator().removeCallback(communicatorCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void appearLoading() {
        int loadingSizePx = GuiTools.convertDpToPixels(activity, LOADING_SIZE_DP);
        CustomAnimator animator = new CustomAnimator();
        Animator animation = animator.createAnimatorSize(loading, 1, 1, loadingSizePx, loadingSizePx, getResources().getInteger(R.integer.durationShort));
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if(loading != null) {
                    loading.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {

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
}
