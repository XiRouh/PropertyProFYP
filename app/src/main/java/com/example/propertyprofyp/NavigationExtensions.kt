package com.example.propertyprofyp

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private lateinit var auth: FirebaseAuth

// Extension function on AppCompatActivity
fun AppCompatActivity.setupBottomNavigation(currentActivity: Class<*>) {
    val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    bottomNavigationView.setOnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.home -> {
                getUserRole { isStaff ->
                    if (isStaff) {
                        navigateTo(StaffDashboardActivity::class.java, currentActivity)
                    } else {
                        navigateTo(ViewPropertyActivity::class.java, currentActivity)
                    }
                }
                true
            }
            R.id.booking -> {
                navigateTo(ViewAppointmentActivity::class.java, currentActivity)
                true
            }
            R.id.purchases -> {
                navigateTo(ViewPurchasesActivity::class.java, currentActivity)
                true
            }
            R.id.wishlist -> {
                navigateTo(ViewWishlistActivity::class.java, currentActivity)
                true
            }
            R.id.profile -> {
                navigateTo(UserProfileActivity::class.java, currentActivity)
                true
            }
            else -> false
        }
    }
}

private fun AppCompatActivity.navigateTo(targetActivity: Class<*>, currentActivity: Class<*>) {
    if (currentActivity != targetActivity) {
        val intent = Intent(this, targetActivity)
        startActivity(intent)
    }
}

fun getUserRole(completion: (Boolean) -> Unit) {
    // Initialize FirebaseAuth instance
    val auth = FirebaseAuth.getInstance()

    val userId = auth.currentUser?.uid ?: return
    val databaseReference = FirebaseDatabase.getInstance().getReference("users")
    databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val userType = snapshot.child("userType").getValue(String::class.java)
            completion(userType == "Staff")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Failed to read user data", error.toException())
            completion(false)
        }
    })
}

