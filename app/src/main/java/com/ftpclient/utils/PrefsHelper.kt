package com.ftpclient.utils

import android.content.Context
import com.ftpclient.model.FtpConfig

class PrefsHelper(context: Context) {

    private val prefs = context.getSharedPreferences("ftp_prefs", Context.MODE_PRIVATE)

    fun saveConfig(config: FtpConfig) {
        prefs.edit()
            .putString("host", config.host)
            .putInt("port", config.port)
            .putString("username", config.username)
            .putString("password", config.password)
            .putBoolean("passive", config.passiveMode)
            .putString("encoding", config.encoding)
            .apply()
    }

    fun loadConfig(): FtpConfig? {
        val host = prefs.getString("host", null) ?: return null
        return FtpConfig(
            host = host,
            port = prefs.getInt("port", 21),
            username = prefs.getString("username", "anonymous") ?: "anonymous",
            password = prefs.getString("password", "") ?: "",
            passiveMode = prefs.getBoolean("passive", true),
            encoding = prefs.getString("encoding", "UTF-8") ?: "UTF-8"
        )
    }

    fun saveDownloadDir(dir: String) {
        prefs.edit().putString("download_dir", dir).apply()
    }

    fun loadDownloadDir(defaultDir: String): String {
        return prefs.getString("download_dir", defaultDir) ?: defaultDir
    }
}
