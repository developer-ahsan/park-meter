package com.parkmeter.og.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.parkmeter.og.fragment.discovery.DiscoveryMethod;
import com.stripe.stripeterminal.external.models.Reader;

import java.util.List;

public class TerminalViewModel extends ViewModel {
    public final MutableLiveData<Boolean> simulated;
    public final MutableLiveData<Integer> discoveryMethodPosition;
    public final MutableLiveData<Boolean> isConnecting;
    public final MutableLiveData<Boolean> isConnected;
    public final MutableLiveData<Reader> connectedReader;
    public final MutableLiveData<String> connectionStatus;
    private List<DiscoveryMethod> discoveryMethods;
    private DiscoveryMethod discoveryMethod;

    public TerminalViewModel(boolean simulated, DiscoveryMethod discoveryMethod, List<DiscoveryMethod> discoveryMethods) {
        this.discoveryMethod = discoveryMethod;
        this.simulated = new MutableLiveData<>(simulated);
        this.discoveryMethods = discoveryMethods;
        this.discoveryMethodPosition = new MutableLiveData<>(discoveryMethods.indexOf(discoveryMethod));
        this.isConnecting = new MutableLiveData<>(false);
        this.isConnected = new MutableLiveData<>(false);
        this.connectedReader = new MutableLiveData<>(null);
        this.connectionStatus = new MutableLiveData<>("Ready to connect");
    }

    public DiscoveryMethod getDiscoveryMethod() {
        return discoveryMethods.get(discoveryMethodPosition.getValue());
    }

    public void setConnecting(boolean connecting) {
        isConnecting.postValue(connecting);
        if (connecting) {
            connectionStatus.postValue("Connecting to Tap to Pay reader...");
        }
    }

    public void setConnected(Reader reader) {
        isConnecting.postValue(false);
        isConnected.postValue(true);
        connectedReader.postValue(reader);
        connectionStatus.postValue("Connected to: " + reader.getSerialNumber());
    }

    public void setDisconnected() {
        isConnecting.postValue(false);
        isConnected.postValue(false);
        connectedReader.postValue(null);
        connectionStatus.postValue("Reader disconnected");
    }

    public void setConnectionFailed(String error) {
        isConnecting.postValue(false);
        isConnected.postValue(false);
        connectedReader.postValue(null);
        connectionStatus.postValue("Connection failed: " + error);
    }

    public void setPermissionRequired() {
        isConnecting.postValue(false);
        isConnected.postValue(false);
        connectedReader.postValue(null);
        connectionStatus.postValue("Location and Internet permissions required");
    }

    public void setNoInternet() {
        isConnecting.postValue(false);
        isConnected.postValue(false);
        connectedReader.postValue(null);
        connectionStatus.postValue("No internet connection available");
    }
}
