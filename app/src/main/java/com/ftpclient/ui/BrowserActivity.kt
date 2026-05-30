package com.ftpclient.ui

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ftpclient.databinding.ActivityBrowserBinding
import com.ftpclient.model.FtpEntry
import com.ftpclient.utils.PrefsHelper
import com.ftpclient.viewmodel.BrowserState
import com.ftpclient.viewmodel.BrowserViewModel
import com.ftpclient.viewmodel.BrowserViewModelFactory
import com.ftpclient.viewmodel.DownloadState

class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private lateinit var viewModel: BrowserViewModel
    private lateinit var adapter: FileListAdapter
    private lateinit var prefs: PrefsHelper

    // Available download directories
    private val downloadDirs by lazy {
        listOf(
            "Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath,
            "Music"     to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
            "Pictures"  to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
            "Movies"    to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
        )
    }

    private var downloadDir: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsHelper(this)

        val defaultDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ).absolutePath
        downloadDir = prefs.loadDownloadDir(defaultDir)

        val ftpManager = FtpManagerHolder.ftpManager ?: run { finish(); return }
        val factory = BrowserViewModelFactory(ftpManager)
        viewModel = ViewModelProvider(this, factory)[BrowserViewModel::class.java]

        setupRecyclerView()
        setupToolbar()
        observeStates()
        updateDirLabel()

        if (savedInstanceState == null) {
            viewModel.navigateTo("/")
        }
    }

    private fun setupRecyclerView() {
        adapter = FileListAdapter(
            onDirectoryClick = { entry -> viewModel.navigateTo(entry.path) },
            onFileClick = { entry -> confirmDownload(entry) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BrowserActivity)
            adapter = this@BrowserActivity.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupToolbar() {
        binding.btnUp.setOnClickListener {
            if (viewModel.canGoUp) viewModel.navigateUp()
        }
        binding.btnRefresh.setOnClickListener {
            viewModel.refresh()
        }
        binding.btnChooseDir.setOnClickListener {
            showDirectoryPicker()
        }
        setSupportActionBar(binding.toolbar)
    }

    private fun observeStates() {
        viewModel.browserState.observe(this) { state ->
            when (state) {
                is BrowserState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    updatePathBar(viewModel.currentPath)
                }
                is BrowserState.Listing -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    updatePathBar(state.path)
                    binding.btnUp.isEnabled = viewModel.canGoUp
                    if (state.entries.isEmpty()) {
                        binding.recyclerView.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                        adapter.submitList(state.entries)
                    }
                }
                is BrowserState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                    updatePathBar(viewModel.currentPath)
                }
            }
        }

        viewModel.downloadState.observe(this) { state ->
            when (state) {
                is DownloadState.Idle -> hideDownloadOverlay()
                is DownloadState.Downloading -> showDownloadProgress(state)
                is DownloadState.Success -> {
                    hideDownloadOverlay()
                    Toast.makeText(this, "Saved: ${state.localPath}", Toast.LENGTH_LONG).show()
                    viewModel.resetDownloadState()
                }
                is DownloadState.Error -> {
                    hideDownloadOverlay()
                    Toast.makeText(this, "Download failed: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetDownloadState()
                }
            }
        }
    }

    private fun updatePathBar(path: String) {
        binding.tvPath.text = path
        supportActionBar?.title = path.substringAfterLast('/').ifBlank { "/" }
    }

    private fun updateDirLabel() {
        val name = downloadDirs.find { it.second == downloadDir }?.first ?: "Custom"
        binding.btnChooseDir.text = "[$name]"
    }

    private fun confirmDownload(entry: FtpEntry) {
        AlertDialog.Builder(this)
            .setTitle("Download File")
            .setMessage("Download \"${entry.name}\" (${entry.displaySize}) to $downloadDir ?")
            .setPositiveButton("Download") { _, _ ->
                viewModel.downloadFile(entry, downloadDir)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDirectoryPicker() {
        val labels = downloadDirs.map { (name, path) ->
            if (path == downloadDir) ">> $name <<" else name
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Download Directory")
            .setItems(labels) { _, index ->
                downloadDir = downloadDirs[index].second
                prefs.saveDownloadDir(downloadDir)
                updateDirLabel()
                Toast.makeText(this, "Save to: ${downloadDirs[index].first}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showDownloadProgress(state: DownloadState.Downloading) {
        val p = state.progress
        binding.downloadOverlay.visibility = View.VISIBLE
        binding.tvDownloadName.text = "Downloading: ${p.fileName}"
        binding.downloadProgress.progress = p.percentage
        binding.tvDownloadPercent.text = if (p.totalBytes > 0)
            "${p.percentage}%  (${formatBytes(p.bytesTransferred)} / ${formatBytes(p.totalBytes)})"
        else
            formatBytes(p.bytesTransferred)
    }

    private fun hideDownloadOverlay() {
        binding.downloadOverlay.visibility = View.GONE
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

    override fun onBackPressed() {
        if (viewModel.canGoUp) {
            viewModel.navigateUp()
        } else {
            super.onBackPressed()
        }
    }
}
