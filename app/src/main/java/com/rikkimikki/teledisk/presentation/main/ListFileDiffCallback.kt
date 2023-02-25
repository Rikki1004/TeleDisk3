package com.rikkimikki.teledisk.presentation.main

import androidx.recyclerview.widget.DiffUtil
import com.rikkimikki.teledisk.domain.TdObject
import com.rikkimikki.teledisk.domain.Tfile
import com.rikkimikki.teledisk.domain.Tfolder


object ListFileDiffCallback : DiffUtil.ItemCallback<TdObject>() {

    override fun areItemsTheSame(oldItem: TdObject, newItem: TdObject): Boolean {
        return true
        return if (oldItem is Tfile && newItem is Tfile)
            (oldItem as Tfile).name == (newItem as Tfile).name
        else if (oldItem is Tfolder && newItem is Tfolder)
            (oldItem as Tfolder).name == (newItem as Tfolder).name
        else false
    }

    override fun areContentsTheSame(oldItem: TdObject, newItem: TdObject): Boolean {
        return true
    }
}