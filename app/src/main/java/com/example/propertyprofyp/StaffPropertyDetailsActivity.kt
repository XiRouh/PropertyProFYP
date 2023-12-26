package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StaffPropertyDetailsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var propertyType: TextView
    private lateinit var tenureType: TextView
    private lateinit var areaType: TextView
    private lateinit var location: TextView
    private lateinit var buildUp: TextView
    private lateinit var price: TextView
    private lateinit var description: TextView
    private lateinit var projectName: TextView
    private lateinit var developer: TextView
    private lateinit var completionDate: TextView
    private lateinit var maintenanceFee: TextView
    private lateinit var packageInfo: TextView
    private lateinit var viewPager: ViewPager
    private lateinit var indicatorsContainer: LinearLayout
    private lateinit var deleteIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.staff_view_property_details)

        viewPager = findViewById(R.id.propertyMediaViewPager)
        indicatorsContainer = findViewById(R.id.indicatorsContainer)

        initializeViews()
        val propertyId = intent.getStringExtra("propertyId")
        if (propertyId != null) {
            fetchPropertyDetails(propertyId)
        } else {
            // Handle the case where propertyId is null
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        propertyType = findViewById(R.id.propertyType)
        tenureType = findViewById(R.id.tenureType)
        areaType = findViewById(R.id.areaType)
        location = findViewById(R.id.location)
        buildUp = findViewById(R.id.buildUp)
        price = findViewById(R.id.price)
        description = findViewById(R.id.description)
        projectName = findViewById(R.id.projectName)
        developer = findViewById(R.id.developer)
        completionDate = findViewById(R.id.completionDate)
        maintenanceFee = findViewById(R.id.maintenanceFee)
        packageInfo = findViewById(R.id.packages)

        val videosBar: TextView = findViewById(R.id.videosBar)
        videosBar.setOnClickListener {
            val propertyId = intent.getStringExtra("propertyId")
            val intent = Intent(this, EditPropertyVideoActivity::class.java)
            intent.putExtra("propertyId", propertyId)
            intent.putExtra("isViewOnly", true) // Indicate that this is view-only mode
            startActivity(intent)
        }

        val propertyId = intent.getStringExtra("propertyId")

        deleteIcon = findViewById(R.id.deleteIcon)
        deleteIcon.setOnClickListener {
            propertyId?.let { id ->
                deleteProperty(id)
            } ?: run {
                // Handle case where propertyId is null or not found
            }
        }

        val editBtn: Button = findViewById(R.id.editBtn)
        editBtn.setOnClickListener {
            val intent = Intent(this, EditPropertyDetailsActivity::class.java)
            intent.putExtra("propertyId", propertyId) // assuming propertyId is the ID of the current property
            startActivity(intent)
        }

        // Initialize ViewPager and Indicators
        viewPager = findViewById(R.id.propertyMediaViewPager)
        indicatorsContainer = findViewById(R.id.indicatorsContainer)
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
            }
        })
    }

    private fun fetchPropertyDetails(propertyId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.child(propertyId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val property = snapshot.getValue(Property::class.java)
                property?.let { updateUI(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun updateUI(property: Property) {
        // Assume property has a list of media URLs
        val mediaUrls = property.getAllMediaUrls() // Implement this to return a list of URLs

        viewPager.adapter = MediaPagerAdapter(mediaUrls)
        setupIndicators(mediaUrls.size)

        propertyType.text = property.propertyType
        tenureType.text = property.tenure
        areaType.text = property.area
        location.text = property.location
        buildUp.text = property.buildUp.toString() + "sq.ft."
        price.text = "RM " + property.price.toString()
        description.text = property.description
        projectName.text = property.projectName
        developer.text = property.developer
        completionDate.text = property.completionDate
        maintenanceFee.text = "RM " + property.maintenanceFee.toString() + "per month"
        packageInfo.text = property.packageInfo

        // Set up click listener for documentBar to view documents
    }

    private fun setupIndicators(count: Int) {
        val indicators = Array(count) { TextView(this) }
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i].layoutParams = layoutParams
            indicators[i].text = Html.fromHtml("&#8226;") // HTML code for bullet symbol
            indicators[i].textSize = 35f
            indicators[i].setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            indicatorsContainer.addView(indicators[i])
        }

        updateIndicators(0) // Set first indicator active
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until indicatorsContainer.childCount) {
            val color = if (i == position) android.R.color.black else android.R.color.darker_gray
            (indicatorsContainer.getChildAt(i) as TextView).setTextColor(ContextCompat.getColor(this, color))
        }
    }

    private fun deleteProperty(propertyId: String) {
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (loggedInUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog before proceeding with deletion
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this property?")
            .setPositiveButton("Delete") { dialog, _ ->
                // User confirmed the deletion
                performDeletion(propertyId, loggedInUserId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // User cancelled the action
                dialog.dismiss()
            }
            .show()
    }

    private fun performDeletion(propertyId: String, loggedInUserId: String) {
        // Check if user has delete permission
        checkDeletePermission(loggedInUserId) { hasPermission ->
            if (hasPermission) {
                // User has permission to delete, proceed with deletion
                val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
                databaseReference.child(propertyId).removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Property deleted successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ViewPropertyActivity::class.java)
                        startActivity(intent)
                        finish() // Close the current activity
                    } else {
                        Toast.makeText(this, "Failed to delete property", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // User does not have permission to delete
                Toast.makeText(this, "You do not have permission to delete this property", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkDeletePermission(userId: String, callback: (Boolean) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("permissions")
        userRef.get().addOnSuccessListener { dataSnapshot ->
            val permissions = dataSnapshot.getValue<Map<String, Boolean>>()
            val canDelete = permissions?.get("removeProperty") == true
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
