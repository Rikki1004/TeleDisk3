package com.rikkimikki.teledisk.presentation.main

import androidx.recyclerview.widget.DiffUtil
import com.rikkimikki.teledisk.domain.baseClasses.TdObject


object ListFileDiffCallback : DiffUtil.ItemCallback<TdObject>() {

    override fun areItemsTheSame(oldItem: TdObject, newItem: TdObject): Boolean {
        return oldItem.fileID == newItem.fileID
    }

    override fun areContentsTheSame(oldItem: TdObject, newItem: TdObject): Boolean {
        return oldItem == newItem
    }

}