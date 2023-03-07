package com.rikkimikki.teledisk.presentation.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.databinding.FileItemBinding
import com.rikkimikki.teledisk.databinding.FileItemGridBinding
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
) : ListAdapter<TdObject, RecyclerView.ViewHolder>(ListFileDiffCallback){



    private val scope:CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val repository = TelegramRepository

    private val loadThumbnailUseCase = LoadThumbnailUseCase(repository)

    val MANAGER_LINEAR = "linear"
    val MANAGER_GRID = "grid"

    var layoutManagerType = MANAGER_LINEAR
    set(value) {
        field = value
    }

    var onFileClickListener: OnFileClickListener? = null
    var onFileLongClickListener: OnFileLongClickListener? = null


    private inner class View1ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        var itemIcon: ImageView = itemView.findViewById(R.id.item_icon)
        var itemName: TextView = itemView.findViewById(R.id.item_name)
        var itemDetails: TextView = itemView.findViewById(R.id.item_details)
        var itemDate: TextView = itemView.findViewById(R.id.item_date)
        fun bind(position: Int) {
            val item = currentList[position]
            if (item.isChecked){
                itemView.setBackgroundColor(context.getColor(R.color.activated_item_foreground))
            }
            else{
                itemView.setBackgroundColor(context.getColor(R.color.colorMainBackground))
            }

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
                }
                else{
                    if (item.placeType == PlaceType.Local)
                        Picasso.get().load(File(item.path)).placeholder(resId).fit().into(itemIcon)
                    else
                        Picasso.get().load(resId).into(itemIcon)
                }

            } else {
                val resId = R.drawable.folder_asset
                itemName.text = item.name
                itemDate.text = covertTimestampToTime(item.unixTimeDate)
                Picasso.get().load(resId).into(itemIcon)
            }
            itemView.setOnClickListener {
                onFileClickListener?.onFileClick(item)
            }
            itemView.setOnLongClickListener {
                onFileLongClickListener?.onFileLongClick(item)
                return@setOnLongClickListener true//false
            }
        }
    }

    private inner class View2ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var itemIcon: ImageView = itemView.findViewById(R.id.item_icon_grid)
        var itemName: TextView = itemView.findViewById(R.id.item_name_grid)
        fun bind(position: Int) {
            val item = currentList[position]
            if (item.isChecked)
                itemView.setBackgroundColor(context.getColor(R.color.activated_item_foreground))
            else
                itemView.setBackgroundColor(context.getColor(R.color.colorMainBackground))
            if (item.is_file()) {
                val resId = R.drawable.file_asset
                itemName.text = item.name
                if (item.previewFile != null){
                    scope.launch {
                        val preview = loadThumbnailUseCase(item.previewFile)
                        Picasso.get().load(File(preview.local.path)).into(itemIcon)
                    }
                }
                else{
                    if (item.placeType == PlaceType.Local)
                        Picasso.get().load(File(item.path)).placeholder(resId).fit().into(itemIcon)
                    else
                        Picasso.get().load(resId).into(itemIcon)
                }

            } else {
                val resId = R.drawable.folder_asset
                itemName.text = item.name
                Picasso.get().load(resId).into(itemIcon)
            }
            itemView.setOnClickListener {
                onFileClickListener?.onFileClick(item)
            }
            itemView.setOnLongClickListener {
                onFileLongClickListener?.onFileLongClick(item)
                return@setOnLongClickListener true//false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (layoutManagerType == MANAGER_LINEAR) {
            return View1ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.file_item, parent, false)
            )
        }
        return View2ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.file_item_grid, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (layoutManagerType == MANAGER_LINEAR) {
            (holder as View1ViewHolder).bind(position)
        } else {
            (holder as View2ViewHolder).bind(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (layoutManagerType == MANAGER_LINEAR) {
            1
        } else {
            2
        }
    }

    /*override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListFilesViewHolder {
        val binding = FileItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListFilesViewHolder(binding)
    }*/

    /*override fun onBindViewHolder(holder: ListFilesViewHolder, position: Int) {

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
    }*/

    interface OnFileClickListener {
        fun onFileClick(tdObject: TdObject)
    }
    interface OnFileLongClickListener {
        fun onFileLongClick(tdObject: TdObject)
    }


    private var notFilteredList =  listOf<TdObject>()

    override fun submitList(list: MutableList<TdObject>?) {
        super.submitList(list)
        notFilteredList = list?.toList()?: listOf()
    }


    fun filter1(filter: String?){
        if (filter == null){
            super.submitList(notFilteredList)
            return
        }
        val char = filter.trim()
        if (char.isBlank()){
            super.submitList(notFilteredList)
            return
        }

        super.submitList(notFilteredList.filter {
            if (it.name == "..")
                true
            else
                it.name.lowercase().contains(char.lowercase())
        }.toMutableList())
    }

}
