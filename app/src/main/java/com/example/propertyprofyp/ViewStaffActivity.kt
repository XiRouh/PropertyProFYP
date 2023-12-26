package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ViewStaffActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var staffAdapter: StaffAdapter
    private val staffList: MutableList<User> = mutableListOf()
    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_staff)

        recyclerView = findViewById(R.id.home_property)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val toolbarNotificationIcon = findViewById<ImageView>(R.id.toolbarNotificationIcon)
        toolbarNotificationIcon.setOnClickListener {
            val intent = Intent(this, ViewNotificationActivity::class.java)
            startActivity(intent)
        }

        staffAdapter = StaffAdapter(staffList, { user ->
            val intent = Intent(this, UpdatePermissionActivity::class.java)
            intent.putExtra("userId", user.id)
            startActivity(intent)
        }, { user ->
            deleteStaff(user.id)
        })
        recyclerView.adapter = staffAdapter

        searchView = findViewById(R.id.search)
        setupSearchView()

        loadStaff()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // Return true if the query has been handled by the listener
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterStaffList(newText ?: "")
                return true
            }
        })
    }

    private fun filterStaffList(query: String) {
        val filteredList = if (query.isNotEmpty()) {
            staffList.filter {
                it.id.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }.toMutableList()
        } else {
            staffList
        }
        staffAdapter.updateList(filteredList)
    }

    private fun loadStaff() {
        databaseRef.orderByChild("userType").equalTo("Staff").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                staffList.clear()
                val specialUserList = mutableListOf<User>()
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        if (user.id == "A6kUfKMEKsRJpMTXLHi1XfsWQxu1") {
                            specialUserList.add(user)
                        } else {
                            staffList.add(user)
                        }
                    }
                }
                // Put the special user at the top
                staffList.addAll(0, specialUserList)
                staffAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun deleteStaff(userId: String) {
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (loggedInUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Delete Staff")
            .setMessage("Are you sure you want to delete this staff member?")
            .setPositiveButton("Delete") { dialog, _ ->
                // User confirmed the deletion
                performStaffDeletion(userId, loggedInUserId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // User canceled the deletion
                dialog.dismiss()
            }
            .show()
    }

    private fun performStaffDeletion(userId: String, loggedInUserId: String) {
        checkDeletePermission(loggedInUserId) { hasPermission ->
            if (hasPermission) {
                databaseRef.child(userId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Staff member deleted successfully", Toast.LENGTH_SHORT).show()
                        loadStaff() // Refresh the list after deletion
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete staff member", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "You do not have permission to delete staff", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkDeletePermission(userId: String, callback: (Boolean) -> Unit) {
        val userRef = databaseRef.child(userId).child("permissions")
        userRef.get().addOnSuccessListener { dataSnapshot ->
            val permissions = dataSnapshot.getValue<Map<String, Boolean>>()
            val canDelete = permissions?.get("deleteStaff") == true
            callback(canDelete)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load permissions", Toast.LENGTH_SHORT).show()
            callback(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // Go back to the previous Activity
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
