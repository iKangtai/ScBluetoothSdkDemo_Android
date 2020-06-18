package com.example.blesdkdemo;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * desc
 *
 * @author xiongyl 2020/6/18 20:47
 */
public class HomeViewModel extends ViewModel {
    private MutableLiveData<Boolean> isSearching = new MutableLiveData<>();
    private MutableLiveData<Boolean> isConnecting = new MutableLiveData<>();
    private MutableLiveData<Boolean> isConnect = new MutableLiveData<>();
    private MutableLiveData<StringBuffer> consoleContent = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsSearching() {
        return isSearching;
    }

    public MutableLiveData<Boolean> getIsConnecting() {
        return isConnecting;
    }

    public MutableLiveData<Boolean> getIsConnect() {
        return isConnect;
    }

    public MutableLiveData<StringBuffer> getConsoleContent() {
        return consoleContent;
    }
}
