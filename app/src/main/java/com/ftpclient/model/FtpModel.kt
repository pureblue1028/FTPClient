package com.ftpclient.model

data class FtpConfig(
    val host: String,
    val port: Int = 21,
    val username: String,
    val password: String,
    val passiveMode: Boolean = true,
    val encoding: String = "UTF-8"
)

data class FtpEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val modified: String = ""
) {
    val displaySize: String
        get() = when {
            isDirectory -> "—"
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
}

sealed class FtpResult<out T> {
    data class Success<T>(val data: T) : FtpResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : FtpResult<Nothing>()
}

data class DownloadProgress(
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long
) {
    val percentage: Int
        get() = if (totalBytes > 0) ((bytesTransferred * 100) / totalBytes).toInt() else 0
}
