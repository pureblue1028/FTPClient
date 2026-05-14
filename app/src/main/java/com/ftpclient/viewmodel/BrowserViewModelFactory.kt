package com.ftpclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ftpclient.network.FtpClientManager

class BrowserViewModelFactory(private val ftpManager: FtpClientManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(ftpManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
