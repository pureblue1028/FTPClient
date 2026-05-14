package com.ftpclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ftpclient.model.DownloadProgress
import com.ftpclient.model.FtpEntry
import com.ftpclient.model.FtpResult
import com.ftpclient.network.FtpClientManager
import kotlinx.coroutines.launch

sealed class BrowserState {
    object Loading : BrowserState()
    data class Listing(val path: String, val entries: List<FtpEntry>) : BrowserState()
    data class Error(val message: String) : BrowserState()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: DownloadProgress) : DownloadState()
    data class Success(val fileName: String, val localPath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class BrowserViewModel(val ftpManager: FtpClientManager) : ViewModel() {

    private val _browserState = MutableLiveData<BrowserState>()
    val browserState: LiveData<BrowserState> = _browserState

    private val _downloadState = MutableLiveData<DownloadState>(DownloadState.Idle)
    val downloadState: LiveData<DownloadState> = _downloadState

    private val pathStack = ArrayDeque<String>()

    val currentPath: String
        get() = pathStack.lastOrNull() ?: "/"

    val canGoUp: Boolean
        get() = pathStack.size > 1

    fun navigateTo(path: String) {
        pathStack.addLast(path)
        loadCurrentDirectory()
    }

    fun navigateUp() {
        if (pathStack.size > 1) {
            pathStack.removeLast()
            loadCurrentDirectory()
        }
    }

    fun refresh() {
        loadCurrentDirectory()
    }

    private fun loadCurrentDirectory() {
        val path = currentPath
        _browserState.value = BrowserState.Loading
        viewModelScope.launch {
            when (val result = ftpManager.listDirectory(path)) {
                is FtpResult.Success -> _browserState.value = BrowserState.Listing(path, result.data)
                is FtpResult.Error -> _browserState.value = BrowserState.Error(result.message)
            }
        }
    }

    fun downloadFile(entry: FtpEntry, localDir: String) {
        if (_downloadState.value is DownloadState.Downloading) return

        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(
                DownloadProgress(entry.name, 0L, entry.size)
            )
            when (val result = ftpManager.downloadFile(
                remotePath = entry.path,
                localDir = localDir,
                fileName = entry.name,
                onProgress = { transferred, total ->
                    _downloadState.postValue(
                        DownloadState.Downloading(
                            DownloadProgress(entry.name, transferred, total)
                        )
                    )
                }
            )) {
                is FtpResult.Success -> _downloadState.value =
                    DownloadState.Success(entry.name, result.data.absolutePath)
                is FtpResult.Error -> _downloadState.value = DownloadState.Error(result.message)
            }
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun disconnect() {
        viewModelScope.launch {
            ftpManager.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
