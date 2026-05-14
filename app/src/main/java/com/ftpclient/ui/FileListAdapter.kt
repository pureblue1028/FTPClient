package com.ftpclient.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ftpclient.databinding.ItemFileBinding
import com.ftpclient.model.FtpEntry

class FileListAdapter(
    private val onDirectoryClick: (FtpEntry) -> Unit,
    private val onFileClick: (FtpEntry) -> Unit
) : ListAdapter<FtpEntry, FileListAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: FtpEntry) {
            binding.tvName.text = entry.name
            binding.tvMeta.text = if (entry.isDirectory) {
                "Directory"
            } else {
                "${entry.displaySize}  ${entry.modified}"
            }
            binding.tvIcon.text = if (entry.isDirectory) "[DIR]" else "[FILE]"

            binding.root.setOnClickListener {
                if (entry.isDirectory) onDirectoryClick(entry)
                else onFileClick(entry)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FtpEntry>() {
        override fun areItemsTheSame(a: FtpEntry, b: FtpEntry) = a.path == b.path
        override fun areContentsTheSame(a: FtpEntry, b: FtpEntry) = a == b
    }
}
