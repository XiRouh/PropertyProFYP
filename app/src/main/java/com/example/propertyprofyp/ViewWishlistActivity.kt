package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ViewWishlistActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var wishlistAdapter: WishlistAdapter
    private var wishlistItems: MutableList<WishlistItem> = mutableListOf()
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_wishlist)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val toolbarNotificationIcon = findViewById<ImageView>(R.id.toolbarNotificationIcon)
        toolbarNotificationIcon.setOnClickListener {
            val intent = Intent(this, ViewNotificationActivity::class.java)
            startActivity(intent)
        }

        // Initialize Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set up the bottom navigation
        setupBottomNavigation(ViewWishlistActivity::class.java)

        recyclerView = findViewById(R.id.home_property)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchWishlist()

        // Get the logged-in user's ID
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        getUserRole { isStaff ->
            if (isStaff) {
                checkUserPermission(loggedInUserId)
            }
        }
    }

    private fun checkUserPermission(loggedInUserId: String?) {
        loggedInUserId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
            databaseRef.get().addOnSuccessListener { dataSnapshot ->
                // Update how permissions are retrieved from the dataSnapshot
                val permissions = dataSnapshot.getValue<Map<String, Boolean>>() ?: emptyMap()
                if (permissions["viewClientWishlistReport"] != true) {
                    Toast.makeText(this, "You do not have permission to view client wishlist", Toast.LENGTH_LONG).show()
                    finish() // Close the activity and return to the previous screen
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load your permissions", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "User not found", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fetchWishlist() {
        getUserRole { isStaff ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val wishlistRef = databaseReference.child("wishlist")

            wishlistRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wishlistItems.clear()
                    for (wishlistSnapshot in snapshot.children) {
                        val wishlist = wishlistSnapshot.getValue(Wishlist::class.java)
                        if (wishlist != null) {
                            if (isStaff || wishlist.userId == userId) {
                                // Fetch additional details like username and project name
                                fetchAdditionalDetails(wishlist)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, "Error fetching wishlist: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchAdditionalDetails(wishlist: Wishlist) {
        val userRef = databaseReference.child("users").child(wishlist.userId)
        val projectRef = databaseReference.child("properties").child(wishlist.projectId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val user = userSnapshot.getValue(User::class.java)
                projectRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(projectSnapshot: DataSnapshot) {
                        val project = projectSnapshot.getValue(Property::class.java)
                        if (user != null && project != null) {
                            val wishlistItem = WishlistItem(
                                id = wishlist.id,
                                userId = wishlist.userId,
                                userName = user.username,
                                projectId = wishlist.projectId,
                                projectName = project.projectName,
                                price = project.price,
                                area = wishlist.area,
                                propertyType = wishlist.propertyType,
                                tenureType = wishlist.tenureType
                            )
                            wishlistItems.add(wishlistItem)
                            updateRecyclerView()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle possible errors
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun updateRecyclerView() {
        wishlistAdapter = WishlistAdapter(wishlistItems) { item ->
            navigateToPropertyDetails(item.projectId)
        }
        recyclerView.adapter = wishlistAdapter
    }

    private fun navigateToPropertyDetails(propertyId: String) {
        getUserRole { isStaff ->
            runOnUiThread {
                val intent = if (isStaff) {
                    Intent(this, StaffPropertyDetailsActivity::class.java)
                } else {
                    Intent(this, UserPropertyDetailsActivity::class.java)
                }

                intent.putExtra("propertyId", propertyId)
                startActivity(intent)
            }
        }
    }

    private fun getUserRole(completion: (Boolean) -> Unit) {
        // Assuming you have the current user's ID. Replace with your method of getting user ID.
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userType = snapshot.child("userType").getValue(String::class.java)
                val isStaff = userType == "Staff"
                completion(isStaff)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user data", error.toException())
                completion(false)
            }
        })
    }
}
