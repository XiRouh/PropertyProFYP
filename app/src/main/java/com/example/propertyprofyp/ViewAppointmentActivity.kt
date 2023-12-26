package com.example.propertyprofyp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewAppointmentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private var bookings: MutableList<Booking> = mutableListOf()
    private val usernameCache = mutableMapOf<String, String>()
    private lateinit var searchView: SearchView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var statusSpinner: Spinner
    private lateinit var timeRangeSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_booking)

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
        setupBottomNavigation(ViewAppointmentActivity::class.java)

        recyclerView = findViewById(R.id.bookingList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        statusSpinner = findViewById(R.id.statusSpinner)
        setupStatusSpinner()

        timeRangeSpinner = findViewById(R.id.timeRangeSpinner)
        setupTimeRangeSpinner()

        getUserRole { isStaff ->
            bookingAdapter = BookingAdapter(bookings, { booking ->
                // Handle change appointment click
                val intent = Intent(this, EditBookingActivity::class.java)
                intent.putExtra("bookingId", booking.id)
                startActivity(intent)
            }, { booking ->
                // Handle update status click
                updateBookingStatus(booking)
            }, { booking ->
                cancelBooking(booking) },
                isStaff, this::fetchUsernameById)

            recyclerView.adapter = bookingAdapter

            searchView = findViewById(R.id.searchBooking)
            setupSearchView()

            fetchBookings()
        }
    }

    private fun setupStatusSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.booking_status_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusSpinner.adapter = adapter

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedStatus = parent.getItemAtPosition(position).toString()
                fetchBookings(statusFilter = selectedStatus)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Nothing to do here
            }
        }
    }

    private fun setupTimeRangeSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.time_range_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeRangeSpinner.adapter = adapter

        timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedTimeRange = parent.getItemAtPosition(position).toString()
                fetchBookings(timeRange = selectedTimeRange)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Nothing to do here
            }
        }
    }

    private fun fetchUsernameById(userId: String, callback: (String) -> Unit) {
        if (usernameCache.containsKey(userId)) {
            callback(usernameCache[userId] ?: "Unknown")
            return
        }

        FirebaseDatabase.getInstance().getReference("users")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    usernameCache[userId] = username
                    callback(username)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to read user data", databaseError.toException())
                    callback("Unknown")
                }
            })
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchBookings(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { fetchBookings(it) }
                return false
            }
        })
    }

    private fun fetchBookings(query: String = "", statusFilter: String = "All", timeRange: String = "All Time") {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")

        val currentCalendar = Calendar.getInstance()
        Log.d("ViewAppointment", "Selected Time Range: $timeRange")

        // Prepare the filterCalendar based on the selected time range
        val filterCalendar = when (timeRange) {
            "This Week" -> Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
            "This Month" -> Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
            "This Year" -> Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
            "More than This Year" -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, 1)
                add(Calendar.YEAR, +1)
            }
            else -> Calendar.getInstance().apply { time = Date(0) } // Distant past for 'All Time'
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        Log.d("ViewAppointment", "Filter Date: ${dateFormat.format(filterCalendar.time)}")

        getUserRole { isStaff ->
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    bookings.clear()
                    for (postSnapshot in dataSnapshot.children) {
                        val booking = postSnapshot.getValue(Booking::class.java)
                        if (booking != null && (isStaff || booking.userId == userId)) {
                            val isWithinTimeRange = booking.matchesTimeRange(filterCalendar, timeRange)
                            Log.d("ViewAppointment", "Booking Date: ${booking.date}, Within Time Range: $isWithinTimeRange")
                            if (booking.matchesQuery(query) && booking.matchesStatus(statusFilter) && isWithinTimeRange) {
                                bookings.add(booking)
                            }
                        }
                    }
                    bookingAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors.
                }
            })
        }
    }

    private fun Booking.matchesTimeRange(filterCalendar: Calendar, timeRange: String): Boolean {
        val bookingCalendar = Calendar.getInstance()
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            bookingCalendar.time = dateFormat.parse(this.date) ?: return false
        } catch (e: ParseException) {
            return false
        }

        return when (timeRange) {
            "This Week" -> bookingCalendar.after(filterCalendar) && bookingCalendar.before(Calendar.getInstance())
            "This Month" -> {
                val startOfMonth = Calendar.getInstance()
                startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
                bookingCalendar.after(startOfMonth) && bookingCalendar.before(Calendar.getInstance())
            }
            "This Year" -> {
                val startOfYear = Calendar.getInstance()
                startOfYear.set(Calendar.DAY_OF_YEAR, 1)
                bookingCalendar.after(startOfYear) && bookingCalendar.before(Calendar.getInstance())
            }
            "More than This Year" -> bookingCalendar.after(filterCalendar)
            else -> true // "All Time"
        }
    }

    private fun Booking.matchesStatus(statusFilter: String): Boolean {
        return when (statusFilter) {
            "All" -> true
            else -> status.equals(statusFilter, ignoreCase = true)
        }
    }

    private fun Booking.matchesQuery(query: String): Boolean {
        if (query.isEmpty()) return true
        return id.contains(query, ignoreCase = true) ||
                projectName.contains(query, ignoreCase = true) ||
                status.contains(query, ignoreCase = true) ||
                date.contains(query, ignoreCase = true) ||
                time.contains(query, ignoreCase = true) ||
                usernameCache[userId]?.contains(query, ignoreCase = true) == true ||
                usernameCache[staffId]?.contains(query, ignoreCase = true) == true ||
                address.contains(query, ignoreCase = true)
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

    private fun updateBookingStatus(booking: Booking) {
        if (booking.status == "Pending" || booking.status == "Done - Without Purchase Intention") {
            val options = arrayOf("Done - With Purchase Intention", "Done - Without Purchase Intention")
            AlertDialog.Builder(this)
                .setTitle("Update Booking Status")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> addPurchaseIntention(booking)
                        1 -> updateBookingStatusInDatabase(booking, "Done - Without Purchase Intention")
                    }
                    dialog.dismiss()
                }
                .show()
        } else {
            Toast.makeText(this, "Only pending or 'Done - Without Purchase Intention' bookings can have their status updated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPurchaseIntention(booking: Booking) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.orderByChild("projectName").equalTo(booking.projectName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val property = snapshot.children.first().getValue(Property::class.java)
                    if (property != null) {
                        val purchase = Purchase(
                            bookingId = booking.id,
                            userId = booking.userId,
                            staffId = booking.staffId,
                            projectName = booking.projectName,
                            projectId = property.id,
                            price = property.price,
                            loanDSR = 0.0
                        )
                        addPurchaseToDatabase(purchase)
                        updateBookingStatusInDatabase(booking, "Done - With Purchase Intention")
                    } else {
                        Log.e("Firebase", "Property not found or null")
                    }
                } else {
                    Log.e("Firebase", "Property snapshot does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read property data", error.toException())
            }
        })
    }

    private fun addPurchaseToDatabase(purchase: Purchase) {
        val purchaseRef = FirebaseDatabase.getInstance().getReference("purchases")
        purchaseRef.child(purchase.bookingId).setValue(purchase).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "Purchase intention added successfully")
            } else {
                Log.e("Firebase", "Failed to add purchase intention: ${task.exception?.message}")
            }
        }
    }

    private fun updateBookingStatusInDatabase(booking: Booking, newStatus: String) {
        val updatedBooking = booking.copy(status = newStatus)
        val bookingRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingRef.child(booking.id).setValue(updatedBooking).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Booking status updated to $newStatus", Toast.LENGTH_SHORT).show()
                sendStatusUpdateNotification(booking.staffId, booking.userId, "Booking Status Updated", "The status of your booking has been updated to $newStatus.")
                fetchBookings()
            } else {
                Toast.makeText(this, "Failed to update booking status: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendStatusUpdateNotification(staffId: String, userId: String, title: String, message: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userToken = snapshot.child(userId).child("token").getValue(String::class.java)
                val staffToken = snapshot.child(staffId).child("token").getValue(String::class.java)

                userToken?.let { sendNotification(Announcement(title, message), it) }
                staffToken?.let { sendNotification(Announcement(title, message), it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user tokens", error.toException())
            }
        })
    }

    private fun cancelBooking(booking: Booking) {
        if (booking.status == "Pending") {
            AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes") { dialog, _ ->
                    proceedWithCancellation(booking)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            Toast.makeText(this, "Only pending status bookings can be cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedWithCancellation(booking: Booking) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")
        databaseReference.child(booking.id).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cancelExistingReminder(booking.id)
                // Send notification to both staff and user
                sendCancellationNotification(booking.staffId, booking.userId, "Booking Cancelled", "Your booking has been cancelled.")
                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                fetchBookings() // Refresh the bookings list
            } else {
                Toast.makeText(this, "Failed to cancel booking: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelExistingReminder(bookingId: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AppointmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, bookingId.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pendingIntent?.let {
            alarmManager.cancel(it)
            Log.d("ViewAppointmentActivity", "Reminder for booking $bookingId cancelled")
        }
    }

    private fun sendCancellationNotification(staffId: String, userId: String, title: String, message: String) {
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

    private fun sendNotification(announcement: Announcement, token: String) = CoroutineScope(Dispatchers.IO).launch {
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
}
