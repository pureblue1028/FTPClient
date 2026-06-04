package com.ftpclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ftpclient.model.FtpConfig
import com.ftpclient.model.FtpResult
import com.ftpclient.network.FtpClientManager
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Connecting : LoginState()
    data class Success(val config: FtpConfig) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    val ftpManager = FtpClientManager()

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun connect(
        host: String,
        portStr: String,
        username: String,
        password: String,
        passive: Boolean,
        encoding: String = "UTF-8"
    ) {
        val port = portStr.toIntOrNull()
        if (host.isBlank()) {
            _loginState.value = LoginState.Error("Host cannot be empty")
            return
        }
        if (port == null || port !in 1..65535) {
            _loginState.value = LoginState.Error("Invalid port number (1 - 65535)")
            return
        }

        val config = FtpConfig(
            host = host.trim(),
            port = port,
            username = username.trim().ifBlank { "anonymous" },
            password = password,
            passiveMode = passive,
            encoding = encoding
        )

        _loginState.value = LoginState.Connecting

        viewModelScope.launch {
            when (val result = ftpManager.connect(config)) {
                is FtpResult.Success -> _loginState.value = LoginState.Success(config)
                is FtpResult.Error -> _loginState.value = LoginState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
