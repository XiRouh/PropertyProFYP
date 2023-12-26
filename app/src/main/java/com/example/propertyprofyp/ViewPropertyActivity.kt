package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class ViewPropertyActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var propertyAdapter: PropertyAdapter
    private var properties: MutableList<Property> = mutableListOf()
    private lateinit var searchView: SearchView
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_home)

        val toolbarNotificationIcon = findViewById<ImageView>(R.id.toolbarNotificationIcon)
        toolbarNotificationIcon.setOnClickListener {
            val intent = Intent(this, ViewNotificationActivity::class.java)
            startActivity(intent)
        }

        // Initialize Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set up the bottom navigation
        setupBottomNavigation(ViewPropertyActivity::class.java)

        recyclerView = findViewById(R.id.home_property)
        recyclerView.layoutManager = LinearLayoutManager(this)

        propertyAdapter = PropertyAdapter(properties) { property ->
            onPropertyClicked(property)
        }
        recyclerView.adapter = propertyAdapter

        searchView = findViewById(R.id.searchProperty)
        setupSearchView()

        fetchProperties()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchProperties(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { searchProperties(it) }
                return true
            }
        })
    }

    private fun fetchProperties() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                properties.clear()
                for (postSnapshot in dataSnapshot.children) {
                    val property = postSnapshot.getValue(Property::class.java)
                    property?.let { properties.add(it) }
                }
                propertyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to fetch properties", databaseError.toException())
            }
        })
    }

    private fun searchProperties(query: String) {
        val filteredProperties = properties.filter {
            it.projectName.contains(query, ignoreCase = true) ||
                    it.area.contains(query, ignoreCase = true) ||
                    it.propertyType.contains(query, ignoreCase = true)
        }
        propertyAdapter.updateProperties(filteredProperties)
    }

    private fun onPropertyClicked(property: Property) {
        getUserRole { isStaff ->
            val intent = if (isStaff) {
                Intent(this, StaffPropertyDetailsActivity::class.java)
            } else {
                Intent(this, UserPropertyDetailsActivity::class.java)
            }
            intent.putExtra("propertyId", property.id)
            startActivity(intent)
        }
    }

    private fun getUserRole(completion: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

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
}
