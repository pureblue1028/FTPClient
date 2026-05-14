package com.ftpclient.ui

import com.ftpclient.network.FtpClientManager

/**
 * Simple singleton to pass the FtpClientManager between Activities
 * without using a process-level Application class for brevity.
 * The LoginViewModel sets this before launching BrowserActivity.
 */
object FtpManagerHolder {
    var ftpManager: FtpClientManager? = null
}
