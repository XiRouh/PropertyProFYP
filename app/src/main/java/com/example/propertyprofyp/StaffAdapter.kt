package com.example.propertyprofyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class StaffAdapter(
    private var staffList: MutableList<User>,
    private val onPermissionUpdateClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {

    fun updateList(newList: MutableList<User>) {
        staffList = newList
        notifyDataSetChanged()
    }

    class StaffViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userIdTextView: TextView = view.findViewById(R.id.userId)
        val userNameTextView: TextView = view.findViewById(R.id.userName)
        val emailTextView: TextView = view.findViewById(R.id.email)
        val permissionsTextView: TextView = view.findViewById(R.id.permission)
        val updateBtn: Button = view.findViewById(R.id.updateBtn)
        val deleteIcon: ImageView = view.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.staff_card_design, parent, false)
        return StaffViewHolder(view)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val staff = staffList[position]

        // Display user data
        holder.userIdTextView.text = "UserID:" + staff.id
        holder.userNameTextView.text = "StaffName: " + staff.username
        holder.emailTextView.text = "Email: " + staff.email

        // Display permissions if they exist and are true
        val permissions = staff.permissions?.entries?.filter { it.value }?.joinToString(", ") { it.key }
        holder.permissionsTextView.text = "Permissions: ${permissions ?: "None"}"

        // Disable buttons for specific userId
        if (staff.id == "A6kUfKMEKsRJpMTXLHi1XfsWQxu1") {
            holder.updateBtn.isEnabled = false
            holder.deleteIcon.isVisible = false
        } else {
            holder.updateBtn.setOnClickListener { onPermissionUpdateClick(staff) }
            holder.deleteIcon.setOnClickListener { onDeleteClick(staff) }
        }
    }

    override fun getItemCount() = staffList.size
}
