package com.rikkimikki.teledisk.presentation.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.FileItemBinding
import com.rikkimikki.teledisk.domain.TdObject
import com.rikkimikki.teledisk.domain.Tfile
import com.rikkimikki.teledisk.domain.Tfolder
import com.rikkimikki.teledisk.utils.covertTimestampToTime
import com.squareup.picasso.Picasso

class ListFilesAdapter (
    private val context: Context
) : ListAdapter<TdObject, ListFilesAdapter.ListFilesViewHolder>(ListFileDiffCallback) {

    class ListFilesViewHolder(
        val binding: FileItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    var onFileClickListener: OnFileClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListFilesViewHolder {
        val binding = FileItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListFilesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListFilesViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            when(item) {
                is Tfile ->{
                    val resId = R.drawable.file_asset
                    itemName.text = item.name
                    itemDate.text = covertTimestampToTime(item.unixTimeDate)
                    Picasso.get().load(resId).into(itemIcon)
                    //Glide.with(context).load(resId).into(imageViewItemFile)
                }
                is Tfolder -> {
                    val resId = R.drawable.folder_asset
                    itemName.text = item.name
                    itemDate.text = covertTimestampToTime(item.unixTimeDate)
                    //Glide.with(context).load(resId).into(imageViewItemFile)
                    Picasso.get().load(resId).into(itemIcon)
                }
            }
            this.root.setOnClickListener {
                onFileClickListener?.onFileClick(item)
            }
        }
    }

    interface OnFileClickListener {
        fun onFileClick(tdObject: TdObject)
    }
}
