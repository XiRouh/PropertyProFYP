package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserPropertyDetailsActivity : AppCompatActivity() {

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
    private lateinit var loveIcon: ImageView
    private lateinit var bookingBtn: Button

    private lateinit var wishlistReference: DatabaseReference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_view_property_details)

        viewPager = findViewById(R.id.propertyMediaViewPager)
        indicatorsContainer = findViewById(R.id.indicatorsContainer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initializeViews()
        val propertyId = intent.getStringExtra("propertyId")
        if (propertyId != null && userId != null) {
            fetchPropertyDetails(propertyId)
            checkWishlistStatus(propertyId!!, userId)
        } else {
            // Handle the case where propertyId is null
        }
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

        loveIcon = findViewById(R.id.loveIcon)

        loveIcon.setOnClickListener { toggleWishlist() }

        bookingBtn = findViewById(R.id.bookingBtn)
        bookingBtn.setOnClickListener {
            val intent = Intent(this, MakeAppointmentActivity::class.java)
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

    private fun checkWishlistStatus(propertyId: String, userId: String) {
        Log.d("Enter checkWishlistStatus", "Enter checkWishlistStatus")

        wishlistReference = FirebaseDatabase.getInstance().getReference("wishlist")
        wishlistReference.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isWishlisted = false
                for (childSnapshot in snapshot.children) {
                    val wishlist = childSnapshot.getValue(Wishlist::class.java)
                    if (wishlist?.projectId == propertyId) {
                        isWishlisted = true
                        break
                    }
                }
                setLoveIcon(isWishlisted)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserPropertyDetailsActivity, "Error checking wishlist status.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setLoveIcon(isWishlisted: Boolean) {
        Log.d("Enter setLoveIcon", "Enter setLoveIcon")

        if (isWishlisted) {
            loveIcon.setImageResource(R.drawable.love_icon_red)
        } else {
            loveIcon.setImageResource(R.drawable.love_icon)
        }
    }

    private fun toggleWishlist() {
        Log.d("UserPropertyDetails", "toggleWishlist: Start")

        val propertyId = intent.getStringExtra("propertyId")

        val projectId = propertyId ?: run {
            Log.e("UserPropertyDetails", "toggleWishlist: propertyId is null")
            return
        }

        val currentUserId = userId ?: run {
            Log.e("UserPropertyDetails", "toggleWishlist: userId is null")
            return
        }

        Log.d("UserPropertyDetails", "toggleWishlist: CurrentUserId: $currentUserId, ProjectId: $projectId")

        wishlistReference = FirebaseDatabase.getInstance().getReference("wishlist")
        wishlistReference.orderByChild("userId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("UserPropertyDetails", "toggleWishlist: onDataChange - Snapshot exists: ${snapshot.exists()}")

                    if (!snapshot.exists()) {
                        // The wishlist node does not exist or is empty, add property to wishlist
                        Log.d("UserPropertyDetails", "toggleWishlist: Wishlist node does not exist or is empty")
                        val newWishlist = Wishlist("", currentUserId, projectId, propertyType.text.toString(), tenureType.text.toString(), areaType.text.toString())
                        addPropertyToWishlist(newWishlist)
                        return
                    }

                    var wishlistId: String? = null
                    for (childSnapshot in snapshot.children) {
                        val wishlist = childSnapshot.getValue(Wishlist::class.java)
                        if (wishlist?.projectId == projectId) {
                            wishlistId = childSnapshot.key
                            break
                        }
                    }

                    if (wishlistId == null) {
                        // Add to wishlist
                        val newWishlist = Wishlist("", currentUserId, projectId, propertyType.text.toString(), tenureType.text.toString(), areaType.text.toString())
                        addPropertyToWishlist(newWishlist)
                    } else {
                        // Remove from wishlist
                        removePropertyFromWishlist(wishlistId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserPropertyDetails", "toggleWishlist: onCancelled - Error: ${error.message}")
                    Toast.makeText(this@UserPropertyDetailsActivity, "Error updating wishlist.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addPropertyToWishlist(wishlist: Wishlist) {
        Log.d("Enter addPropertyToWishlist", "Enter addPropertyToWishlist")

        val newWishlistId = wishlistReference.push().key ?: run {
            Log.e("UserPropertyDetails", "Failed to generate new wishlist ID")
            return
        }

        // Set the generated ID in the wishlist object
        val newWishlistWithId = wishlist.copy(id = newWishlistId)

        wishlistReference.child(newWishlistId).setValue(newWishlistWithId).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Added to wishlist.", Toast.LENGTH_SHORT).show()
                setLoveIcon(true)
            } else {
                Toast.makeText(this, "Failed to add to wishlist.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun removePropertyFromWishlist(wishlistId: String) {
        Log.d("Enter removePropertyFromWishlist", "Enter removePropertyFromWishlist")

        wishlistReference.child(wishlistId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Removed from wishlist.", Toast.LENGTH_SHORT).show()
                setLoveIcon(false)
            } else {
                Toast.makeText(this, "Failed to remove from wishlist.", Toast.LENGTH_SHORT).show()
            }
        }
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
