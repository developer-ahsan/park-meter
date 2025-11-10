package com.parkmeter.og.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.parkmeter.og.NavigationListener;
import com.parkmeter.og.fragment.discovery.DiscoveryMethod;

import com.stripe.stripeterminal.external.callable.Cancelable;

import com.stripe.stripeterminal.external.models.Reader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryViewModel extends ViewModel {
    public final DiscoveryMethod discoveryMethod;
    public final MutableLiveData<List<? extends Reader>> readers;
    public final MutableLiveData<Boolean> isConnecting;
    public final MutableLiveData<Boolean> isUpdating;
    public final MutableLiveData<Float> updateProgress;
    @Nullable
    public Cancelable discoveryTask;

    @Nullable
    public NavigationListener navigationListener;


    public DiscoveryViewModel(@NotNull DiscoveryMethod discoveryMethod) {
        this.discoveryMethod = discoveryMethod;
        readers = new MutableLiveData<>(new ArrayList<>());
        isConnecting = new MutableLiveData<>(false);
        isUpdating = new MutableLiveData<>(false);
        updateProgress = new MutableLiveData<>(0F);
        discoveryTask = null;
    }
}
