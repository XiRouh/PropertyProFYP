package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ViewNotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private var announcements: MutableList<Announcement> = mutableListOf()
    private val databaseReference = FirebaseDatabase.getInstance().getReference("announcements")
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_notification)

        // Initialize Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set up the bottom navigation
        setupBottomNavigation(ViewNotificationActivity::class.java)

        recyclerView = findViewById(R.id.home_property)
        recyclerView.layoutManager = LinearLayoutManager(this)

        notificationAdapter = NotificationAdapter(announcements) { announcement ->
            val intent = Intent(this, ViewNotificationDetailsActivity::class.java)
            intent.putExtra("announcementId", announcement.id)
            startActivity(intent)
        }

        searchView = findViewById(R.id.searchNotification)
        setupSearchView()

        recyclerView.adapter = notificationAdapter

        fetchAnnouncements()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchAnnouncements(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { fetchAnnouncements(it) }
                return true
            }
        })
    }

    private fun fetchAnnouncements(query: String = "") {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userType = snapshot.child("userType").getValue(String::class.java)
                databaseReference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        announcements.clear()
                        for (postSnapshot in dataSnapshot.children) {
                            val announcement = postSnapshot.getValue(Announcement::class.java)
                            if (announcement != null && matchesQuery(announcement, query) &&
                                (announcement.recipientType == "User" || announcement.recipientType == userType)) {
                                announcements.add(announcement)
                            }
                        }
                        notificationAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle possible errors.
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
            }
        })
    }

    private fun matchesQuery(announcement: Announcement, query: String): Boolean {
        return query.isEmpty() ||
                announcement.title.contains(query, ignoreCase = true) ||
                announcement.message.contains(query, ignoreCase = true) ||
                announcement.recipientType.contains(query, ignoreCase = true)
    }
}
