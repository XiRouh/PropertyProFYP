package com.example.propertyprofyp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ViewNotificationDetailsActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var messageTextView: TextView
    private val databaseReference = FirebaseDatabase.getInstance().getReference("announcements")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notification_details)

        titleTextView = findViewById(R.id.titleTextView)
        messageTextView = findViewById(R.id.messageTextView)

        val announcementId = intent.getStringExtra("announcementId") ?: return

        databaseReference.child(announcementId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val announcement = snapshot.getValue(Announcement::class.java)
                if (announcement != null) {
                    titleTextView.text = announcement.title
                    messageTextView.text = announcement.message
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
            }
        })
    }
}
