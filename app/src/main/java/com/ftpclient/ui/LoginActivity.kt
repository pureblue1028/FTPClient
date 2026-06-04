package com.ftpclient.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ftpclient.R
import com.ftpclient.databinding.ActivityLoginBinding
import com.ftpclient.model.FtpConfig
import com.ftpclient.utils.PrefsHelper
import com.ftpclient.viewmodel.LoginState
import com.ftpclient.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var prefs: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsHelper(this)
        prefs.loadConfig()?.let { populateFields(it) }

        binding.btnConnect.setOnClickListener {
            val encoding = when (binding.rgEncoding.checkedRadioButtonId) {
                R.id.rbGbk -> "GBK"
                R.id.rbLatin1 -> "ISO-8859-1"
                else -> "UTF-8"
            }
            viewModel.connect(
                host = binding.etHost.text.toString(),
                portStr = binding.etPort.text.toString(),
                username = binding.etUsername.text.toString(),
                password = binding.etPassword.text.toString(),
                passive = binding.switchPassive.isChecked,
                encoding = encoding
            )
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle -> showIdle()
                is LoginState.Connecting -> showConnecting()
                is LoginState.Success -> onConnected(state.config)
                is LoginState.Error -> showError(state.message)
            }
        }
    }

    private fun populateFields(config: FtpConfig) {
        binding.etHost.setText(config.host)
        binding.etPort.setText(config.port.toString())
        binding.etUsername.setText(config.username)
        binding.etPassword.setText(config.password)
        binding.switchPassive.isChecked = config.passiveMode
        when (config.encoding) {
            "GBK" -> binding.rbGbk.isChecked = true
            "ISO-8859-1" -> binding.rbLatin1.isChecked = true
            else -> binding.rbUtf8.isChecked = true
        }
    }

    private fun showIdle() {
        binding.btnConnect.isEnabled = true
        binding.btnConnect.text = "Connect"
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
    }

    private fun showConnecting() {
        binding.btnConnect.isEnabled = false
        binding.btnConnect.text = "Connecting..."
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.GONE
    }

    private fun onConnected(config: FtpConfig) {
        prefs.saveConfig(config)
        FtpManagerHolder.ftpManager = viewModel.ftpManager
        startActivity(Intent(this, BrowserActivity::class.java))
        viewModel.resetState()
    }

    private fun showError(message: String) {
        binding.btnConnect.isEnabled = true
        binding.btnConnect.text = "Connect"
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = "Error: $message"
    }
}
