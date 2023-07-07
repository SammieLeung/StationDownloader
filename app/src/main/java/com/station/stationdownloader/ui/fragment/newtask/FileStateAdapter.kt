package com.station.stationdownloader.ui.fragment.newtask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.databinding.FileItemBinding
import com.station.stationdownloader.ui.viewmodel.FileTreeModel

class FileStateAdapter(val fileStateList: List<FileTreeModel> = emptyList()) :
    RecyclerView.Adapter<FileStateAdapter.FileViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder.create(parent, viewType)
    }

    override fun getItemCount(): Int {
        return fileStateList.size
    }


    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
    }

    class FileViewHolder(val binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fileStateItem: FileTreeModel) {
            if(fileStateItem is FileTreeModel.File) {
                binding.fileName.setText(fileStateItem.fileName)
                binding.checkbox.isChecked = fileStateItem.isChecked
            }
        }


        companion object {
            fun create(parent: ViewGroup, viewType: Int): FileViewHolder {
                val binding = FileItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FileViewHolder(binding)
            }
        }
    }

}