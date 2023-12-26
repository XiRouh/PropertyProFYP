package com.example.propertyprofyp

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditBookingActivity : AppCompatActivity() {

    private lateinit var bookingIdTextView: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var staffNameTextView: TextView
    private lateinit var bookingDateTimeTextView: TextView
    private lateinit var projectNameTextView: TextView
    private lateinit var bookingAddressTextView: TextView
    private lateinit var bookingStatusTextView: TextView
    private lateinit var dateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var staffSpinner: Spinner
    private lateinit var submitButton: Button
    private var originalStaffId: String? = null

    private var bookingId: String? = null
    private var userId: String? = null
    private val staffIdMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_booking)

        initializeViews()
        bookingId = intent.getStringExtra("bookingId")

        fetchBookingDetails(bookingId)
        setupDatePicker()
        setupTimePicker()
        submitButton.setOnClickListener { updateBooking() }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun initializeViews() {
        // Initialize all your views here
        bookingIdTextView = findViewById(R.id.bookingId)
        userNameTextView = findViewById(R.id.userName)
        staffNameTextView = findViewById(R.id.staffName)
        bookingDateTimeTextView = findViewById(R.id.bookingDateTime)
        projectNameTextView = findViewById(R.id.bookingProject)
        bookingAddressTextView = findViewById(R.id.bookingAddress)
        bookingStatusTextView = findViewById(R.id.bookingStatus)
        dateEditText = findViewById(R.id.dateSelect)
        timeEditText = findViewById(R.id.timeSelect)
        staffSpinner = findViewById(R.id.spinnerStaff)
        submitButton = findViewById(R.id.submitBtn)
    }

    private fun fetchBookingDetails(bookingId: String?) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")
        bookingId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val booking = snapshot.getValue(Booking::class.java)
                    booking?.let { b ->
                        originalStaffId = b.staffId // Store original staff ID
                        displayBookingDetails(b)
                        fetchAvailableStaff(b.date)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditBookingActivity, "Failed to fetch booking details: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun displayBookingDetails(booking: Booking) {
        bookingIdTextView.text = booking.id
        userNameTextView.text = booking.userId
        staffNameTextView.text = booking.staffId
        bookingDateTimeTextView.text = booking.date + " " + booking.time
        projectNameTextView.text = booking.projectName
        bookingAddressTextView.text = booking.address
        bookingStatusTextView.text = booking.status
    }

    private fun setupDatePicker() {
        val dateEditText: EditText = findViewById(R.id.dateSelect)

        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            if (!selectedCalendar.after(today)) {
                // If selected date is before today, show an error and do not proceed
                Toast.makeText(this, "Please select a date later than today's date.", Toast.LENGTH_LONG).show()
            } else {
                // Format the date and set it to the EditText
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateEditText.setText(selectedDate)

                // Clear previous staff availability and fetch new availability
                clearStaffAvailability()
                fetchAvailableStaff(selectedDate)
            }
        }

        dateEditText.setOnClickListener {
            DatePickerDialog(this, datePickerListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun clearStaffAvailability() {
        // Clear the available staff list and refresh the spinner
        val emptyList = mutableListOf<String>()
        updateStaffSpinner(emptyList)
    }

    private fun updateStaffSpinner(availableStaff: MutableList<String>) {
        Log.d("EditAppointment", "Updating spinner with staff: $availableStaff")

        runOnUiThread {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableStaff)
            staffSpinner.adapter = adapter
        }
    }

    private fun fetchAvailableStaff(date: String) {
        Log.d("EditAppointment", "Fetching available staff for date: $date")

        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        val availableStaff = mutableListOf<String>("No prefer staff") // Add 'No prefer staff' initially

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val userType = child.child("userType").getValue(String::class.java)
                    val staffUsername = child.child("username").getValue(String::class.java)
                    if (userType == "Staff" && staffUsername != null) {
                        staffIdMap[staffUsername] = child.key!!  // Save the mapping
                        checkStaffAvailability(child.key!!, staffUsername, date, availableStaff)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("EditAppointment", "Error fetching users: ${error.message}")
            }
        })
    }

    private fun checkStaffAvailability(staffId: String, staffUsername: String, date: String, availableStaff: MutableList<String>) {
        Log.d("EditAppointment", "Checking availability for staff: $staffUsername")

        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.orderByChild("staffId").equalTo(staffId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(bookingSnapshot: DataSnapshot) {
                var isAvailable = true
                bookingSnapshot.children.forEach { booking ->
                    val bookingDate = booking.child("date").getValue(String::class.java)
                    if (bookingDate == date) {
                        Log.d("MakeAppointment", "Staff $staffUsername is not available on $date")
                        isAvailable = false
                        return@forEach
                    }
                }
                if (isAvailable) {
                    Log.d("EditAppointment", "Staff $staffUsername is available on $date")
                    availableStaff.add(staffUsername)
                    updateStaffSpinner(availableStaff)
                }
            }

            override fun onCancelled(bookingError: DatabaseError) {
                // ... existing error handling ...
            }
        })
    }

    private fun setupTimePicker() {
        val timePickerListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            if (hourOfDay in 8..14) {
                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                timeEditText.setText(selectedTime)
            } else {
                Toast.makeText(this, "Select time between 8 AM and 2 PM", Toast.LENGTH_SHORT).show()
            }
        }

        timeEditText.setOnClickListener {
            TimePickerDialog(this, timePickerListener, 8, 0, true).show()
        }
    }

    private fun updateBooking() {
        val updatedDate = dateEditText.text.toString()
        val updatedTime = timeEditText.text.toString()

        val selectedStaffUsername = staffSpinner.selectedItem.toString()

        val updatedStaffId = when {
            selectedStaffUsername == "No prefer staff" -> assignRandomStaff()
            else -> staffIdMap[selectedStaffUsername] ?: "" // Fallback to an empty string if not found
        }

        if (updatedDate.isNotEmpty() && updatedTime.isNotEmpty()) {
            val updatedBooking = Booking(
                id = bookingId ?: "",
                userId = userNameTextView.text.toString(),
                staffId = updatedStaffId,
                date = updatedDate,
                time = updatedTime,
                projectName = projectNameTextView.text.toString(),
                address = bookingAddressTextView.text.toString(),
                status = "Pending"
            )

            val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")
            val currentBookingId = bookingId
            databaseReference.child(currentBookingId ?: return).setValue(updatedBooking).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Send notification to the user
                    sendNotificationToUser(updatedBooking.userId, updatedStaffId, "Booking Updated", "Your booking has been updated.")

                    // Check if the staff member has been changed
                    if (originalStaffId != null && originalStaffId != updatedStaffId) {
                        // Notify original staff
                        originalStaffId.let { originalId ->
                            if (originalId != null) {
                                sendNotificationToUser(originalId,"", "Booking Update", "A booking you were assigned to has been updated.")
                            }
                        }
                    }

                    // Cancel existing reminder
                    currentBookingId?.let { cancelExistingReminder(it) }

                    // Set new reminder
                    setReminderForAppointment(updatedBooking)

                    Toast.makeText(this, "Booking updated successfully", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, ViewAppointmentActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update booking: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Please select new date and time", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelExistingReminder(bookingId: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AppointmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, bookingId.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    private fun setReminderForAppointment(booking: Booking) {
        val reminderTime = calculateReminderTime(booking.date, booking.time)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AppointmentReminderReceiver::class.java).apply {
            putExtra("bookingId", booking.id)
            putExtra("userId", booking.userId)
            putExtra("staffId", booking.staffId)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, booking.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Check if the app can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // For older versions, proceed without the check
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        } else {
            // Handle the case where permission is not granted
            Toast.makeText(this, "Alarm permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateReminderTime(date: String, time: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val appointmentDateTime = format.parse("$date $time") ?: return 0L

        val appointmentTime = Calendar.getInstance().apply {
            timeInMillis = appointmentDateTime.time
            add(Calendar.DAY_OF_MONTH, -3) // 3 days before
            set(Calendar.HOUR_OF_DAY, 4) // 9 AM
            set(Calendar.MINUTE, 19)
        }

        val reminderTimeInMillis = appointmentTime.timeInMillis
        Log.d("MakeAppointment", "Reminder Time: ${format.format(reminderTimeInMillis)}")

        return reminderTimeInMillis
    }

    private fun assignRandomStaff(): String {
        // Logic to randomly select a staff ID from the staffIdMap
        val availableStaffIds = staffIdMap.values.toList()
        return if (availableStaffIds.isNotEmpty()) {
            availableStaffIds.random()
        } else {
            "" // Fallback to an empty string if no staff is available
        }
    }

    private fun sendNotificationToUser(userId: String, staffId: String, title: String, message: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Fetch token for the user
                val userToken = snapshot.child(userId).child("token").getValue(String::class.java)

                // Fetch tokens for original and updated staff
                val originalStaffToken = snapshot.child(originalStaffId ?: "").child("token").getValue(String::class.java)
                val updatedStaffToken = snapshot.child(staffId).child("token").getValue(String::class.java)

                // Send notification to user
                userToken?.let { token ->
                    sendNotification(PushNotification(Announcement(title, message), listOf(token)))
                }

                // Send notification to original staff if changed
                if (originalStaffId != null && originalStaffId != staffId) {
                    originalStaffToken?.let { token ->
                        sendNotification(PushNotification(Announcement("Booking Update", "A booking you were assigned to has been updated."), listOf(token)))
                    }
                }

                // Send notification to updated staff
                if (originalStaffId != staffId) {
                    updatedStaffToken?.let { token ->
                        sendNotification(PushNotification(Announcement("New Booking Assignment", "You have a new or updated booking."), listOf(token)))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user tokens", error.toException())
            }
        })
    }

    private fun fetchAndSendNotification(userId: String, title: String, message: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.child("token").getValue(String::class.java)
                token?.let {
                    val notification = PushNotification(Announcement(title, message), listOf(it))
                    sendNotification(notification)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user token", error.toException())
            }
        })
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.d("Notification", "Notification sent successfully")
            } else {
                Log.e("Notification", "Error sending notification: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("Notification", "Exception in sending notification: ${e.message}")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            showConfirmationDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        showConfirmationDialog()
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit? Any unsaved changes will be lost.")
            .setPositiveButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
