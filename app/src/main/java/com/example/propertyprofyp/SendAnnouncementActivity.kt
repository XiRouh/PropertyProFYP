package com.example.propertyprofyp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

const val TOPIC = "/topics/myTopic"

class SendAnnouncementActivity : AppCompatActivity() {

    private lateinit var titleField: EditText
    private lateinit var messageField: EditText
    private lateinit var recipientSpinner: Spinner
    private lateinit var submitButton: Button
    private val databaseReference = FirebaseDatabase.getInstance().getReference("announcements")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_announcement)

        FirebaseService.sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrEmpty()) {
                FirebaseService.token = token
                // Use the token as needed
            }
        }.addOnFailureListener {
            // Handle any errors
        }

        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

        titleField = findViewById(R.id.titleField)
        messageField = findViewById(R.id.messageField)
        recipientSpinner = findViewById(R.id.spinnerRecipientType)
        submitButton = findViewById(R.id.submitBtn)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

        // Get the logged-in user's ID
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        checkUserPermission(loggedInUserId)

        submitButton.setOnClickListener {
            submitAnnouncement()
        }
    }

    private fun checkUserPermission(loggedInUserId: String?) {
        loggedInUserId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
            databaseRef.get().addOnSuccessListener { dataSnapshot ->
                // Update how permissions are retrieved from the dataSnapshot
                val permissions = dataSnapshot.getValue<Map<String, Boolean>>() ?: emptyMap()
                if (permissions["sendAnnouncement"] != true) {
                    Toast.makeText(this, "You do not have permission to send announcement", Toast.LENGTH_LONG).show()
                    finish() // Close the activity and return to the previous screen
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load your permissions", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "User not found", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun submitAnnouncement() {
        val title = titleField.text.toString().trim()
        val message = messageField.text.toString().trim()
        val recipientType = recipientSpinner.selectedItem.toString()

        if (title.isNotEmpty() && message.isNotEmpty()) {
            val announcementId = UUID.randomUUID().toString()
            // Fetch tokens based on recipient type and send notifications
            fetchRecipientTokens(recipientType) { tokens ->
                Log.d("SendAnnouncement", "Fetched Tokens: $tokens")

                // You might need to batch the tokens if there are too many
                val notification = PushNotification(Announcement(announcementId,title, message), tokens)
                sendNotification(notification)
            }


            val announcement = Announcement(announcementId, title, message, recipientType)
            saveAnnouncementToFirebase(announcement)
        } else {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }

    // Update to fetch tokens based on recipient type
    private fun fetchRecipientTokens(recipientType: String, callback: (List<String>) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tokens = mutableListOf<String>()
                snapshot.children.forEach { userSnapshot ->
                    val userType = userSnapshot.child("userType").getValue(String::class.java)
                    if (userType == recipientType || recipientType == "User") {
                        val token = userSnapshot.child("token").getValue(String::class.java)
                        token?.let { tokens.add(it) }
                    }
                }
                callback(tokens)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read user tokens", error.toException())
            }
        })
    }

    private fun saveAnnouncementToFirebase(announcement: Announcement) {
        databaseReference.child(announcement.id).setValue(announcement).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Announcement sent successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to send announcement: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.d("SendAnnouncement", "Notification sent successfully")
                response.body()?.let { responseBody ->
                    Log.d("SendAnnouncement", "Response Body: ${responseBody.string()}")
                }
            } else {
                Log.e("SendAnnouncement", "Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SendAnnouncement", "Exception in sending notification: ${e.message}")
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
