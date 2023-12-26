package com.example.propertyprofyp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.Manifest
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    companion object {
        private const val PERMISSIONS_REQUEST_READ_STORAGE = 100
        private const val PERMISSIONS_REQUEST_EXACT_ALARM = 101
        private const val PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        auth.currentUser?.let {
            getUserRole { isStaff ->
                redirectToAppropriateActivity(isStaff)
            }
        } ?: run {
            setupLoginAndSignUpButtons()
        }

        // Check and request storage permission on devices running Android 9 or lower
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            checkAndRequestStoragePermission()
        }

        // Check and request exact alarm permission on devices running Android 12 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkAndRequestExactAlarmPermission()
        }

        // Check and request notification permission on devices running Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestNotificationPermission()
        }
    }

    private fun getUserRole(completion: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
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

    private fun redirectToAppropriateActivity(isStaff: Boolean) {
        val intent = if (isStaff) {
            Intent(this, StaffDashboardActivity::class.java)
        } else {
            Intent(this, ViewPropertyActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupLoginAndSignUpButtons() {
        val signupButton = findViewById<Button>(R.id.signup)
        val loginButton = findViewById<Button>(R.id.login)

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_READ_STORAGE)
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM), PERMISSIONS_REQUEST_EXACT_ALARM)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSIONS_REQUEST_POST_NOTIFICATIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == PERMISSIONS_REQUEST_EXACT_ALARM) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Exact Alarm permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Exact Alarm permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}