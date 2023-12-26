package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StaffDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bookingIdTextView: TextView
    private lateinit var bookingDateTimeTextView: TextView
    private lateinit var bookingProjectTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.staff_dashboard)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bookingIdTextView = findViewById(R.id.bookingId)
        bookingDateTimeTextView = findViewById(R.id.bookingDateTime)
        bookingProjectTextView = findViewById(R.id.bookingProject)

        val toolbarNotificationIcon = findViewById<ImageView>(R.id.toolbarNotificationIcon)
        toolbarNotificationIcon.setOnClickListener {
            val intent = Intent(this, ViewNotificationActivity::class.java)
            startActivity(intent)
        }

        // Set up the bottom navigation
        setupBottomNavigation(StaffDashboardActivity::class.java)

        // Fetch and display upcoming booking
        fetchUpcomingBooking()

        val addProperty = findViewById<CardView>(R.id.addPropertyCard)
        val sendAnnouncement = findViewById<CardView>(R.id.sendAnnouncementCard)
        val wishListReport = findViewById<CardView>(R.id.viewWishlistReportCard)
        val manageStaff = findViewById<CardView>(R.id.manageStaffCard)
        val viewProperties = findViewById<CardView>(R.id.propertiesCard)
        val viewSelectedProperty = findViewById<CardView>(R.id.upcomingBookingCard)

        viewSelectedProperty.setOnClickListener {
            val intent = Intent(this, ViewAppointmentActivity::class.java)
            startActivity(intent)
        }

        addProperty.setOnClickListener {
            val intent = Intent(this, AddPropertyDetailsActivity::class.java)
            startActivity(intent)
        }

        sendAnnouncement.setOnClickListener {
            val intent = Intent(this, SendAnnouncementActivity::class.java)
            startActivity(intent)
        }

        wishListReport.setOnClickListener {
            val intent = Intent(this, GenerateReportActivity::class.java)
            startActivity(intent)
        }

        manageStaff.setOnClickListener {
            val intent = Intent(this, ViewStaffActivity::class.java)
            startActivity(intent)
        }

        viewProperties.setOnClickListener {
            val intent = Intent(this, ViewPropertyActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchUpcomingBooking() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                var closestBooking: Booking? = null
                for (postSnapshot in snapshot.children) {
                    val booking = postSnapshot.getValue(Booking::class.java)
                    if (booking != null && booking.date >= currentDate && (closestBooking == null || booking.date < closestBooking.date)) {
                        closestBooking = booking
                    }
                }
                updateBookingCard(closestBooking)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun updateBookingCard(booking: Booking?) {
        booking?.let {
            bookingIdTextView.text = it.id
            bookingDateTimeTextView.text = "${it.date} at ${it.time}"
            bookingProjectTextView.text = it.projectName
        }
    }
}