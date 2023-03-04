package com.rikkimikki.teledisk.presentation.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.databinding.FileItemBinding
import com.rikkimikki.teledisk.domain.GetRemoteFilesUseCase
import com.rikkimikki.teledisk.domain.LoadThumbnailUseCase
import com.rikkimikki.teledisk.domain.PlaceType
import com.rikkimikki.teledisk.domain.TdObject
import com.rikkimikki.teledisk.utils.covertTimestampToTime
import com.rikkimikki.teledisk.utils.humanReadableByteCountSI
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ListFilesAdapter (
    private val context: Context
) : ListAdapter<TdObject, ListFilesAdapter.ListFilesViewHolder>(ListFileDiffCallback) {

    private val scope:CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val repository = TelegramRepository

    private val loadThumbnailUseCase = LoadThumbnailUseCase(repository)

    class ListFilesViewHolder(
        val binding: FileItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    var onFileClickListener: OnFileClickListener? = null
    var onFileLongClickListener: OnFileLongClickListener? = null

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
            if (item.isChecked)
                root.setBackgroundColor(context.getColor(R.color.activated_item_foreground))
            else
                root.setBackgroundColor(context.getColor(R.color.just_item_foreground))
            if (item.is_file()) {
                val resId = R.drawable.file_asset
                itemName.text = item.name
                itemDate.text = covertTimestampToTime(item.unixTimeDate)
                itemDetails.text = humanReadableByteCountSI(item.size)
                if (item.previewFile != null){
                    scope.launch {
                        val preview = loadThumbnailUseCase(item.previewFile)
                        Picasso.get().load(File(preview.local.path)).into(itemIcon)
                    }
                    //Picasso.get().load(File(item.previewFile)).into(itemIcon)
                }
                else{
                    if (item.placeType == PlaceType.Local)
                        Picasso.get().load(File(item.path)).placeholder(resId).fit().into(itemIcon)
                    else
                        Picasso.get().load(resId).into(itemIcon)
                    //Glide.with(context).load(resId).into(itemIcon)
                }

            } else {
                val resId = R.drawable.folder_asset
                itemName.text = item.name
                itemDate.text = covertTimestampToTime(item.unixTimeDate)
                //itemDetails.text = humanReadableByteCountSI(item.size)
                //Glide.with(context).load(resId).into(imageViewItemFile)
                Picasso.get().load(resId).into(itemIcon)
            }
            root.setOnClickListener {
                onFileClickListener?.onFileClick(item)
            }
            root.setOnLongClickListener {
                onFileLongClickListener?.onFileLongClick(item)
                return@setOnLongClickListener true//false
            }

        }
    }

    interface OnFileClickListener {
        fun onFileClick(tdObject: TdObject)
    }
    interface OnFileLongClickListener {
        fun onFileLongClick(tdObject: TdObject)
    }
}
