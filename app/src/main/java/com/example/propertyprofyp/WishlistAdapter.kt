package com.example.propertyprofyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyprofyp.R

class WishlistAdapter(private val wishlistItems: List<WishlistItem>, private val onViewClick: (WishlistItem) -> Unit) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    class WishlistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wishlistId: TextView = view.findViewById(R.id.wishlistId)
        val userId: TextView = view.findViewById(R.id.userId)
        val userName: TextView = view.findViewById(R.id.userName)
        val projectId: TextView = view.findViewById(R.id.projectId)
        val projectName: TextView = view.findViewById(R.id.projectName)
        val area: TextView = view.findViewById(R.id.area)
        val propertyType: TextView = view.findViewById(R.id.propertyType)
        val tenureType: TextView = view.findViewById(R.id.tunureType)
        val viewBtn: Button = view.findViewById(R.id.viewBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wishlist_card_design, parent, false)
        return WishlistViewHolder(view)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val item = wishlistItems[position]
        holder.wishlistId.text = "WishlistID: ${item.id}"
        holder.userId.text = "UserID: ${item.userId}"
        holder.userName.text = "Username: ${item.userName}"
        holder.projectId.text = "ProjectID: ${item.projectId}"
        holder.projectName.text = "Project Name: ${item.projectName}"
        holder.area.text = "Area: ${item.area}"
        holder.propertyType.text = "Property Type: ${item.propertyType}"
        holder.tenureType.text = "Tenure Type: ${item.tenureType}"

        holder.viewBtn.setOnClickListener { onViewClick(item) }
    }

    override fun getItemCount() = wishlistItems.size
}

data class WishlistItem(
    val id: String,
    val userId: String,
    val userName: String,
    val projectId: String,
    val projectName: String,
    val price: Double,
    val area: String,
    val propertyType: String,
    val tenureType: String
)
