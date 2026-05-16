package com.ftpclient.network

import android.util.Log
import com.ftpclient.model.FtpConfig
import com.ftpclient.model.FtpEntry
import com.ftpclient.model.FtpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class FtpClientManager {

    private val ftpClient = FTPClient()
    private val TAG = "FtpClientManager"

    val isConnected: Boolean
        get() = ftpClient.isConnected

    suspend fun connect(config: FtpConfig): FtpResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (ftpClient.isConnected) {
                ftpClient.logout()
                ftpClient.disconnect()
            }

            ftpClient.connectTimeout = 15_000
            ftpClient.soTimeout = 30_000
            ftpClient.defaultTimeout = 15_000

            ftpClient.connect(config.host, config.port)

            val replyCode = ftpClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect()
                return@withContext FtpResult.Error("Server refused connection (code $replyCode)")
            }

            val loginSuccess = ftpClient.login(config.username, config.password)
            if (!loginSuccess) {
                ftpClient.disconnect()
                return@withContext FtpResult.Error("Login failed. Check username and password.")
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            if (config.passiveMode) {
                ftpClient.enterLocalPassiveMode()
            } else {
                ftpClient.enterLocalActiveMode()
            }

            FtpResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            FtpResult.Error("Connection failed: ${e.message}", e)
        }
    }

    suspend fun listDirectory(path: String): FtpResult<List<FtpEntry>> = withContext(Dispatchers.IO) {
        try {
            val files = ftpClient.listFiles(path)
            if (files == null) {
                return@withContext FtpResult.Error("Failed to list directory")
            }

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val entries = files
                .filter { it.name != "." && it.name != ".." }
                .sortedWith(compareByDescending<org.apache.commons.net.ftp.FTPFile> { it.isDirectory }
                    .thenBy { it.name.lowercase() })
                .map { file ->
                    val entryPath = if (path.endsWith("/")) "$path${file.name}"
                    else "$path/${file.name}"
                    val modifiedDate = file.timestamp?.time?.let { dateFormatter.format(it) } ?: ""
                    FtpEntry(
                        name = file.name,
                        path = entryPath,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.size else 0L,
                        modified = modifiedDate
                    )
                }

            FtpResult.Success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "List error", e)
            FtpResult.Error("Failed to list directory: ${e.message}", e)
        }
    }

    suspend fun downloadFile(
        remotePath: String,
        localDir: String,
        fileName: String,
        onProgress: (Long, Long) -> Unit
    ): FtpResult<File> = withContext(Dispatchers.IO) {
        try {
            // Get file size first
            val fileSize = ftpClient.mlistFile(remotePath)?.size ?: 0L

            val localFile = File(localDir, fileName)
            localFile.parentFile?.mkdirs()

            FileOutputStream(localFile).use { outputStream ->
                var transferred = 0L
                ftpClient.retrieveFileStream(remotePath)?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        transferred += bytesRead
                        onProgress(transferred, fileSize)
                    }
                } ?: return@withContext FtpResult.Error("Could not open remote file stream")

                ftpClient.completePendingCommand()
            }

            FtpResult.Success(localFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            FtpResult.Error("Download failed: ${e.message}", e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            if (ftpClient.isConnected) {
                ftpClient.logout()
                ftpClient.disconnect()
            } else {
                // not connected. nothing to do
            }
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }
    }

    fun getParentPath(currentPath: String): String {
        if (currentPath == "/" || currentPath.isEmpty()) return "/"
        val trimmed = currentPath.trimEnd('/')
        val lastSlash = trimmed.lastIndexOf('/')
        return if (lastSlash <= 0) "/" else trimmed.substring(0, lastSlash)
    }
}
