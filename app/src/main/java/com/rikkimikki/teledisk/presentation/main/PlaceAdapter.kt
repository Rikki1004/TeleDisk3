package com.rikkimikki.teledisk.presentation.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.StoreItemBinding
import com.rikkimikki.teledisk.domain.baseClasses.PlaceItem
import com.rikkimikki.teledisk.utils.humanReadableByteCountSI


class PlaceAdapter(private val context: Context) : RecyclerView.Adapter<PlaceAdapter.PlaceItemViewHolder>(){

    var placeItemList: List<PlaceItem> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onPlaceClickListener:OnPlaceClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.store_item,parent,false)
        return PlaceItemViewHolder(view)
    }

    override fun getItemCount() = placeItemList.size

    override fun onBindViewHolder(holder: PlaceItemViewHolder, position: Int) {
        val place = placeItemList[position]
        with(holder){
            tvName.text = place.name
            tvOccupatedSpace.text = humanReadableByteCountSI(place.occupatedSpace)
            pbFreeSpace.max = 100
            if (place.totalSpase == 0L)
                pbFreeSpace.progress = 100
            else{
                pbFreeSpace.progress = (place.occupatedSpace/place.totalSpase.toFloat()*100).toInt()
                tvTotalSpace.text = "| "+humanReadableByteCountSI(place.totalSpase)
            }


            if (place.isMain)
                //itemView.setBackgroundColor(context.getColor(R.color.colorStoreBackgroundMain))
                itemView.background = AppCompatResources.getDrawable(context,R.drawable.rounded_primary_corner_big_main)
            else
                //itemView.setBackgroundColor(context.getColor(R.color.colorStoreBackgroundOther))
                itemView.background = AppCompatResources.getDrawable(context,R.drawable.rounded_primary_corner_big)

            itemView.setOnClickListener {
                onPlaceClickListener?.onPlaceClick(place)
            }
        }
    }

    inner class PlaceItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        private var viewBinding =  StoreItemBinding.bind(itemView)
        val tvName = viewBinding.mainScopeTextViewName
        val tvOccupatedSpace = viewBinding.mainScopeTextViewOccupatedSpace
        val tvTotalSpace = viewBinding.mainScopeTextViewTotalSpace
        val pbFreeSpace = viewBinding.mainScopeProgressBarFreeSpace
    }

    interface OnPlaceClickListener{
        fun onPlaceClick(placeItem: PlaceItem)
    }
}
