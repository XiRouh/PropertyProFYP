package com.example.propertyprofyp

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

class MakeAppointmentActivity : AppCompatActivity() {

    private lateinit var projectNameTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var dateEditText: EditText
    private lateinit var timeSpinner: Spinner
    private lateinit var staffSpinner: Spinner
    private lateinit var submitButton: Button

    private var propertyId: String? = null
    private val staffIdMap = mutableMapOf<String, String>()
    private var availableSessionsMap = mutableMapOf<String, List<String>>()

    private val sessionDisplayMap = mapOf(
        "Morning Session: 8AM - 11AM" to "8:00",
        "Morning Session: 9AM - 11AM" to "9:00",
        "Morning Session: 10AM - 11AM" to "10:00",
        "Afternoon Session: 2PM - 5PM" to "14:00",
        "Afternoon Session: 3PM - 5PM" to "15:00",
        "Afternoon Session: 4PM - 5PM" to "16:00"
    )
    private val staffAvailabilityMap = mutableMapOf<String, MutableList<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.make_appointment)

        projectNameTextView = findViewById(R.id.bookingProject)
        addressTextView = findViewById(R.id.bookingAddress)
        dateEditText = findViewById(R.id.dateSelect)
        timeSpinner = findViewById(R.id.timeSelect)
        staffSpinner = findViewById(R.id.spinnerStaff)
        submitButton = findViewById(R.id.submitBtn)

        propertyId = intent.getStringExtra("propertyId")
        fetchPropertyDetails(propertyId)

        staffSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedStaff = parent.getItemAtPosition(position).toString()
                if (selectedStaff == "No prefer staff") {
                    updateAvailableSessionsForNoPreference()
                } else {
                    updateAvailableSessions(selectedStaff)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        setupDatePicker()
        setupTimeSpinner()

        submitButton.setOnClickListener {
            submitBooking()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun fetchPropertyDetails(propertyId: String?) {
        propertyId ?: return

        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.child(propertyId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val property = snapshot.getValue(Property::class.java)
                property?.let {
                    projectNameTextView.text = it.projectName
                    addressTextView.text = it.location
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Error fetching property details: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAvailableSessionsForNoPreference() {
        val allAvailableSessions = mutableListOf<String>()
        staffAvailabilityMap.values.forEach { sessions ->
            allAvailableSessions.addAll(sessions)
        }
        val uniqueAvailableSessions = allAvailableSessions.distinct()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uniqueAvailableSessions)
        timeSpinner.adapter = adapter
    }

    private fun fetchAvailableStaff(date: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        staffAvailabilityMap.clear()

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val userType = child.child("userType").getValue(String::class.java)
                    val staffUsername = child.child("username").getValue(String::class.java)
                    if (userType == "Staff" && staffUsername != null) {
                        staffIdMap[staffUsername] = child.key!!  // Save the mapping
                        checkStaffAvailability(child.key!!, staffUsername, date)
                    }
                }
                updateStaffSpinner(staffIdMap.keys.toMutableList().apply { add(0, "No prefer staff") })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MakeAppointment", "Error fetching users: ${error.message}")
            }
        })
    }

    private fun checkStaffAvailability(staffId: String, staffUsername: String, date: String) {
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.orderByChild("staffId").equalTo(staffId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val morningSessions = mutableListOf("Morning Session: 8AM - 11AM", "Morning Session: 9AM - 11AM", "Morning Session: 10AM - 11AM")
                val afternoonSessions = mutableListOf("Afternoon Session: 2PM - 5PM", "Afternoon Session: 3PM - 5PM", "Afternoon Session: 4PM - 5PM")
                var morningSessionBooked = false
                var afternoonSessionBooked = false

                snapshot.children.forEach { booking ->
                    val bookingDate = booking.child("date").getValue(String::class.java)
                    val bookingTime = booking.child("time").getValue(String::class.java)

                    if (bookingDate == date) {
                        when (bookingTime) {
                            "8:00", "9:00", "10:00" -> morningSessionBooked = true
                            "14:00", "15:00", "16:00" -> afternoonSessionBooked = true
                        }
                    }
                }

                val availableSessions = mutableListOf<String>()
                if (!morningSessionBooked) availableSessions.addAll(morningSessions)
                if (!afternoonSessionBooked) availableSessions.addAll(afternoonSessions)

                if (availableSessions.isNotEmpty()) {
                    staffAvailabilityMap[staffUsername] = availableSessions
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // ... existing error handling ...
            }
        })
    }

    private fun setupTimeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sessionDisplayMap.keys.toList())
        timeSpinner.adapter = adapter
    }

    private fun updateAvailableSessions(staffUsername: String) {
        val availableSessions = staffAvailabilityMap[staffUsername] ?: listOf("No available sessions")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableSessions)
        timeSpinner.adapter = adapter
    }

    private fun updateStaffSpinner(availableStaff: MutableList<String>) {
        Log.d("MakeAppointment", "Updating spinner with staff: $availableStaff")

        runOnUiThread {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableStaff)
            staffSpinner.adapter = adapter
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            if (!selectedCalendar.after(today)) {
                Toast.makeText(this, "Please select a date later than today's date.", Toast.LENGTH_LONG).show()
            } else {
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateEditText.setText(selectedDate)
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

    private fun populateTimeSpinner(selectedStaff: String) {
        val sessions = availableSessionsMap[selectedStaff] ?: listOf("No available session")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sessions)
        timeSpinner.adapter = adapter
    }

    private fun submitBooking() {
        // Validate date and time inputs
        if (dateEditText.text.isNullOrEmpty() || timeSpinner.selectedItem == null) {
            Toast.makeText(this, "Please enter date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedSession = timeSpinner.selectedItem.toString()
        val selectedTime = sessionDisplayMap[selectedSession]
        val selectedStaffUsername = staffSpinner.selectedItem.toString()
        val selectedStaffId = if (selectedStaffUsername == "No prefer staff") {
            assignRandomStaff()
        } else {
            staffIdMap[selectedStaffUsername]
        }

        val booking = Booking(
            id = UUID.randomUUID().toString(),
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            staffId = selectedStaffId ?: "",
            date = dateEditText.text.toString(),
            time = selectedTime.toString(),
            projectName = projectNameTextView.text.toString(),
            address = addressTextView.text.toString(),
            status = "Pending"
        )

        Log.d("GsonSerialization", "Preparing to serialize booking: " + booking.toString());
        val gson = Gson()
        val json = gson.toJson(booking)
        Log.d("GsonSerialization", "Serialized JSON: " + json)

        val bookingRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingRef.child(booking.id).setValue(booking).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sendNotificationToUserAndStaff(booking.userId, booking.staffId, "New Booking", "You have a new booking.")
                Toast.makeText(this, "Booking successful", Toast.LENGTH_SHORT).show()
                setReminderForAppointment(booking)

                val intent = Intent(this, ViewAppointmentActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Booking failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
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
            set(Calendar.HOUR_OF_DAY, 20) // 9 AM
            set(Calendar.MINUTE, 33)
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
            "" // Handle case where no staff is available
        }
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

    private fun sendNotificationToUserAndStaff(userId: String, staffId: String, title: String, message: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userToken = snapshot.child(userId).child("token").getValue(String::class.java)
                val staffToken = snapshot.child(staffId).child("token").getValue(String::class.java)

                Log.d("SendAnnouncement", "Fetched user Tokens: $userToken")
                Log.d("SendAnnouncement", "Fetched staff Tokens: $staffToken")



                userToken?.let { token ->
                    sendNotification(PushNotification(Announcement(title, message), listOf(token)))
                }
                staffToken?.let { token ->
                    sendNotification(PushNotification(Announcement(title, message), listOf(token)))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user tokens", error.toException())
            }
        })
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
