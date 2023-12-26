package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewPurchasesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var purchaseAdapter: PurchaseAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var searchView: SearchView
    private var purchases: MutableList<Purchase> = mutableListOf()
    private val usernameCache = mutableMapOf<String, String>() // Define usernameCache here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_purchases)

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
        setupBottomNavigation(ViewPurchasesActivity::class.java)

        searchView = findViewById(R.id.searchBooking)
        setupSearchView()

        purchaseAdapter = PurchaseAdapter(purchases, ::onPurchaseInteraction, ::getUserById)
        setupRecyclerView()
        fetchPurchases()
    }

    private fun onPurchaseInteraction(purchase: Purchase, interactionType: PurchaseAdapter.InteractionType) {
        when (interactionType) {
            PurchaseAdapter.InteractionType.CALCULATE_LOAN_DSR -> calculateLoanDSR(purchase)
            PurchaseAdapter.InteractionType.CANCEL_PURCHASE -> cancelPurchase(purchase)
        }
    }

    private fun getUserById(userId: String, callback: (String) -> Unit) {
        if (usernameCache.containsKey(userId)) {
            callback(usernameCache[userId] ?: "Unknown")
            return
        }

        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                callback(username)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching user", error.toException())
                callback("Unknown")
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.bookingList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = purchaseAdapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchPurchases(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { fetchPurchases(it) }
                return true
            }
        })
    }

    private fun fetchPurchases(query: String = "") {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val purchasesRef = FirebaseDatabase.getInstance().getReference("purchases")

        getUserRole { isStaff ->
            purchasesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    purchases.clear()
                    for (purchaseSnapshot in snapshot.children) {
                        val purchase = purchaseSnapshot.getValue(Purchase::class.java)
                        if (purchase != null && (isStaff || purchase.userId == userId)) {
                            if (purchase.matchesQuery(query)){
                                purchases.add(purchase)
                            }
                        }
                    }
                    purchaseAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching purchases", error.toException())
                }
            })
        }
    }

    private fun Purchase.matchesQuery(query: String): Boolean {
        if (query.isEmpty()) return true
        return bookingId.contains(query, ignoreCase = true) ||
                projectName.contains(query, ignoreCase = true)
    }

    private fun getUserRole(completion: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return completion(false)

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

    private fun calculateLoanDSR(purchase: Purchase) {
        val intent = Intent(this, CalculateDSRActivity::class.java)
        intent.putExtra("bookingId", purchase.bookingId)
        startActivity(intent)
    }

    private fun cancelPurchase(purchase: Purchase) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Purchase")
            .setMessage("Are you sure you want to cancel this purchase?")
            .setPositiveButton("Yes") { dialog, _ ->
                proceedWithPurchaseCancellation(purchase)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun proceedWithPurchaseCancellation(purchase: Purchase) {
        // First, update the corresponding booking status
        val bookingRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingRef.child(purchase.bookingId).get().addOnSuccessListener { dataSnapshot ->
            val booking = dataSnapshot.getValue(Booking::class.java)
            booking?.let {
                val updatedBooking = it.copy(status = "Done - Without Purchase Intention")
                bookingRef.child(it.id).setValue(updatedBooking).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Booking status updated to Done - Without Purchase Intention", Toast.LENGTH_SHORT).show()

                        // Then, remove the purchase
                        val purchasesRef = FirebaseDatabase.getInstance().getReference("purchases")
                        purchasesRef.child(purchase.bookingId).removeValue().addOnCompleteListener { removeTask ->
                            if (removeTask.isSuccessful) {
                                sendPurchaseCancellationNotification(purchase.staffId, purchase.userId, "Purchase Cancelled", "Your purchase has been cancelled.")
                                Toast.makeText(this, "Purchase cancelled successfully", Toast.LENGTH_SHORT).show()
                                fetchPurchases() // Refresh the purchases list
                            } else {
                                Toast.makeText(this, "Failed to cancel purchase: ${removeTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to update booking status: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Error updating booking status", e)
            Toast.makeText(this, "Error updating booking status: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPurchaseCancellationNotification(staffId: String, userId: String, title: String, message: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userToken = snapshot.child(userId).child("token").getValue(String::class.java)
                val staffToken = snapshot.child(staffId).child("token").getValue(String::class.java)

                // Prepare notifications for user and staff
                userToken?.let { token -> sendNotification(Announcement(title, message), token) }
                staffToken?.let { token -> sendNotification(Announcement(title, message), token) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user tokens", error.toException())
            }
        })
    }

    private fun sendNotification(announcement: Announcement, token: String) = CoroutineScope(
        Dispatchers.IO).launch {
        try {
            val notification = PushNotification(announcement, listOf(token))
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.d("Notification", "Notification sent successfully")
            } else {
                Log.e("Notification", "Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("Notification", "Exception in sending notification: ${e.message}")
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
