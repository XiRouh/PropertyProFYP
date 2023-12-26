package com.example.propertyprofyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PropertyAdapter(private var properties: List<Property>, private val onPropertyClick: (Property) -> Unit) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val projectName: TextView = view.findViewById(R.id.projectName)
        val price: TextView = view.findViewById(R.id.price)
        val area: TextView = view.findViewById(R.id.area)
        val coverImage: ImageView = view.findViewById(R.id.propertyImg)
        val propertyType: TextView = view.findViewById(R.id.propertyType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_card_design, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        holder.itemView.setOnClickListener { onPropertyClick(property) }
        holder.projectName.text = property.projectName
        holder.price.text = "RM ${property.price}"
        holder.area.text = property.area
        holder.propertyType.text = property.propertyType

        val coverImageUrl = property.coverImage
        Glide.with(holder.coverImage.context).load(coverImageUrl).into(holder.coverImage)
    }

    override fun getItemCount() = properties.size

    fun updateProperties(newProperties: List<Property>) {
        properties = newProperties
        notifyDataSetChanged()
    }
}
