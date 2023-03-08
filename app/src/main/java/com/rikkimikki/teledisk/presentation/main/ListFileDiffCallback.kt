package com.rikkimikki.teledisk.presentation.main

import androidx.recyclerview.widget.DiffUtil
import com.rikkimikki.teledisk.domain.TdObject


object ListFileDiffCallback : DiffUtil.ItemCallback<TdObject>() {

    override fun areItemsTheSame(oldItem: TdObject, newItem: TdObject): Boolean {
        return oldItem.fileID == newItem.fileID
        //return (oldItem as Tfile).fileID == (newItem as Tfile).fileID
        //return true
        /*return if (oldItem is Tfile && newItem is Tfile)
            (oldItem as Tfile).name == (newItem as Tfile).name
        else if (oldItem is Tfolder && newItem is Tfolder)
            (oldItem as Tfolder).name == (newItem as Tfolder).name
        else false*/
    }

    override fun areContentsTheSame(oldItem: TdObject, newItem: TdObject): Boolean {
        return oldItem == newItem
        //return (oldItem as Tfile).fileID == (newItem as Tfile).fileID
    }

}