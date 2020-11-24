### BluetoothCommunicator example

This repository contains an open source sample application of the BluetoothCommunicator library created for RTranslator.
The library, using Bluetooth Low Energy, allows you to communicate in P2P mode between two or more android devices.

The appplication is a Bluetooth chat, when the app is open in the first screen (search) it discovers nearby devices with the app open in the same search screen and shows them in a list containing the code number name of the phones (the name is customizable in the library). 
When the user clicks on one of them the app sends a connection request to that phone and if the latter accepts it, both apps start the chat screen where the two devices cand send text messages (the library can send also raw data) to each other. 
When one of the user press the back button the connection stops and the apps will return to the search screen.
<br /><br />
Galaxy Note 10             |  Galaxy Note 8            |  Galaxy Note 10          
:-------------------------:|:-------------------------:|:-------------------------:
![Connection screen](https://github.com/niedev/BluetoothCommunicatorExample/blob/main/images/Screenshot_pairing.jpg)  |  ![Chat screen 1](https://github.com/niedev/BluetoothCommunicatorExample/blob/main/images/Screenshot_chat1.jpg) | ![Chat screen 2](https://github.com/niedev/BluetoothCommunicatorExample/blob/main/images/Screenshot_chat2.jpg)

<br /><br />
### BluetoothCommunicator library

BluetoothCommunicator is a library that, using Bluetooth Low Energy, allows you to communicate in P2P mode between two or more android devices.<br /><br />
BluetoothCommunicator was created for <a href="https://github.com/niedev/RTranslator" target="_blank" rel="noopener noreferrer">RTranslator</a> but can be used in any more generic case where a P2P communication system is needed between two or more android devices (up to about 4 with a direct connection between all devices, even more with a star structure), for an example app see this repository or <a href="https://github.com/niedev/RTranslator" target="_blank" rel="noopener noreferrer">RTranslator</a><br /><br />

BluetoothCommunicator automatically implements (they are active by default) reconnection in case of temporary connection loss, reliable message sending, splitting and rebuilding of long messages, sending raw data in addition to text messages and a message queue in order to always send the messages (and always in the right order) even in case of connection problems (they will be sent as soon as the connection is restored)

#### Tutorial
For use the library in a project you have to add jitpack.io to your root build.gradle (project):
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Then add the last version of BluetoothCommunicator to your app build.gradle
```
dependencies {
        implementation 'com.github.niedev:BluetoothCommunicator:1.0.5'
}
```

To use this library add these permissions to your manifest:
```
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```
Then add android:largeHeap="true" to the application tag in the manifest:<br />
Example
```
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<application
    android:name="com.bluetooth.communicatorexample.Global"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:largeHeap="true"
    android:theme="@style/Theme.Speech">
    <activity android:name="com.bluetooth.communicatorexample.MainActivity"
        android:configChanges="orientation|screenSize">

        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

After the installation of the library and the changes to the manifest is time to write the code: create a bluetooth communicator object, it is the object that handles all operations of bluetooth low energy library, if you want to manage the bluetooth connections in multiple activities I suggest you to save this object as an attribute of a custom class that extends Application and create a getter so you can access to bluetoothCommunicator from any activity or service with:
```
((custom class name) getApplication()).getBluetoothCommunicator();
```
Next step is to initialize bluetoothCommunicator, the parameters are: a context, the name by which the other devices will see us (limited to 18 characters and can be only characters listed in BluetoothTools.getSupportedUTFCharacters(context) because the number of bytes for advertising beacon is limited) and the strategy (for now the only supported strategy is BluetoothCommunicator.STRATEGY_P2P_WITH_RECONNECTION)
```
bluetoothCommunicator = new BluetoothCommunicator(this, "device name", BluetoothCommunicator.STRATEGY_P2P_WITH_RECONNECTION);
```
Then add the bluetooth communicator callback, the callback will listen for all events of bluetooth communicator:
```
bluetoothCommunicator.addCallback(new BluetoothCommunicator.Callback() {
    @Override
    public void onBluetoothLeNotSupported() {
        super.onBluetoothLeNotSupported();
        
        Notify that bluetooth low energy is not compatible with this device
    }
   
    @Override
    public void onAdvertiseStarted() {
        super.onAdvertiseStarted();
        Notify that advertise has started, if you want to do something after the start of advertising do it here, because
        after startAdvertise there is no guarantee that advertise is really started (it is delayed)
    }

    @Override
    public void onDiscoveryStarted() {
        super.onDiscoveryStarted();

        Notify that discovery has started, if you want to do something after the start of discovery do it here, because
        after startDiscovery there is no guarantee that discovery is really started (it is delayed)
    }

    @Override
    public void onAdvertiseStopped() {
        super.onAdvertiseStopped();

        Notify that advertise has stopped, if you want to do something after the stop of advertising do it here, because
        after stopAdvertising there is no guarantee that advertise is really stopped (it is delayed)
    }

    @Override
    public void onDiscoveryStopped() {
        super.onDiscoveryStopped();

        Notify that discovery has stopped, if you want to do something after the stop of discovery do it here, because
        after stopDiscovery there is no guarantee that discovery is really stopped (it is delayed)
    }

    @Override
    public void onPeerFound(Peer peer) {
        super.onPeerFound(peer);
        
        Here for example you can save peer in a list or anywhere you want and when the user
        choose a peer you can call bluetoothCommunicator.connect(peer founded) but if you want to
        use a peer for connect you have to have peer updated (see onPeerUpdated or onPeerLost), if you use a
        non updated peer the connection might fail
        instead if you want to immediate connect where peer is found you can call bluetoothCommunicator.connect(peer) here
    }

    @Override
    public void onPeerLost(Peer peer){
        super.onPeerLost(peer);
        
        It means that a peer is out of range or has interrupted the advertise,
        here you can delete the peer lost from a eventual collection of founded peers
    }

    @Override
    public void onPeerUpdated(Peer peer,Peer newPeer){
        super.onPeerUpdated(peer,newPeer);

        It means that a founded peer (or connected peer) has changed (name or address or other things),
        if you have a collection of founded peers, you need to replace peer with newPeer if you want to connect successfully to that peer.

        In case the peer updated is connected and you have saved connected peers you have to update the peer if you want to successfully
        send a message or a disconnection request to that peer.
    }

    @Override
    public void onConnectionRequest(Peer peer){
        super.onConnectionRequest(peer);

        It means you have received a connection request from another device (peer) (that have called connect)
        for accept the connection request and start connection call bluetoothCommunicator.acceptConnection(peer);
        for refusing call bluetoothCommunicator.rejectConnection(peer); (the peer must be the peer argument of onConnectionRequest)
    }

    @Override
    public void onConnectionSuccess(Peer peer,int source){
        super.onConnectionSuccess(peer,source);

        This means that you have accepted the connection request using acceptConnection or the other
        device has accepted your connection request and the connection is complete, from now on you
        can send messages or data (or disconnection request) to this peer until onDisconnected

        To send messages to all connected peers you need to create a message with a context, a header, represented by a single character string
        (you can use a header to distinguish between different types of messages, or you can ignore it and use a random
        character), the text of the message, or a series of bytes if you want to send any kind of data and the peer you want to send the message to
        (must be connected to avoid errors), example: new Message(context,"a","hello world",peer);
        If you want to send message to a specific peer you have to set the sender of the message with the corresponding peer.

        To send disconnection request to connected peer you need to call bluetoothCommunicator.disconnect(peer);
    }

    @Override
    public void onConnectionFailed(Peer peer,int errorCode){
        super.onConnectionFailed(peer,errorCode);

        This means that your connection request is rejected or has other problems,
        to know the cause of the failure see errorCode (BluetoothCommunicator.CONNECTION_REJECTED
        means rejected connection and BluetoothCommunicator.ERROR means generic error)
    }

    @Override
    public void onConnectionLost(Peer peer){
        super.onConnectionLost(peer);

        This means that a connected peer has lost the connection with you and the library is trying
        to restore it, in this case you can update the gui to notify this problem.

        You can still send messages in this situation, all sent messages are put in a queue
        and sent as soon as the connection is restored
    }

    @Override
    public void onConnectionResumed(Peer peer){
        super.onConnectionResumed(peer);
        
        Means that connection lost is resumed successfully
    }

    @Override
    public void onMessageReceived(Message message,int source){
        super.onMessageReceived(message,source);

        Means that you have received a message containing TEXT, for know the sender you can call message.getSender() that return
        the peer that have sent the message, you can ignore source, it indicate only if you have received the message
        as client or as server
    }

    @Override
    public void onDataReceived(Message data,int source){
        super.onDataReceived(data,source);

        Means that you have received a message containing DATA, for know the sender you can call message.getSender() that return
        the peer that have sent the message, you can ignore source, it indicate only if you have received the message
        as client or as server
    }

    @Override
    public void onDisconnected(Peer peer,int peersLeft){
        super.onDisconnected(peer,peersLeft);

        Means that the peer is disconnected, peersLeft indicate the number of connected peers remained
    }

    @Override
    public void onDisconnectionFailed(){
        super.onDisconnectionFailed();

        Means that a disconnection is failed, super.onDisconnectionFailed will reactivate bluetooth for forcing disconnection
        (however the disconnection will be notified in onDisconnection)
    }
});
```
Finally you can start discovery and/or advertising:
```
bluetoothCommunicator.startAdvertising();
bluetoothCommunicator.startDiscovery();
```
All other actions that can be done are explained with the comments in the code of callback I wrote before.
For more details see the code of this example app

#### Advanced
For anyone who wants to examinate the library code and generate the .aar file after clone the library on Android Studio:
click on the "Gradle" tab in the right edge of Android Studio, then click on BluetoothCommunicator -> app -> Task -> build -> assemble, then go to the local folder of the BluetoothCommunicator project and click on app -> build -> outputs -> aar, here will be the debug and release .aar files

#### Bugs and problems
Avoid to have installed on your phone multiple apps that use this library, because in that case the bluetooth connection will have problems (maybe it is due to the fact that they are running advertising with the same UUID, try downloading the source files and changing the advertising UUID in the code if you want to try to fix).
In case you have multiple apps using this library, uninstall all but one of them and restart your device in case of problems.
