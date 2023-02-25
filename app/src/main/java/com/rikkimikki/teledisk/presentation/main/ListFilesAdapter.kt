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
        val coin = getItem(position)
        with(holder.binding) {
            with(coin) {
                if (this is Tfile){
                    val resId = R.drawable.file_asset
                    textViewItemFile.text = if (name.length>10) name.substring(0,9) else name
                    Picasso.get().load(resId).into(imageViewItemFile)
                    //Glide.with(context).load(resId).into(imageViewItemFile)
                } else if (this is Tfolder){
                    val resId = R.drawable.folder_asset
                    textViewItemFile.text = if (name.length>10) name.substring(0,9) else name
                    //Glide.with(context).load(resId).into(imageViewItemFile)
                    Picasso.get().load(resId).into(imageViewItemFile)
                }
                root.setOnClickListener {
                    onFileClickListener?.onFileClick(this)
                }
            }
        }
    }

    interface OnFileClickListener {
        fun onFileClick(tdObject: TdObject)
    }
}
