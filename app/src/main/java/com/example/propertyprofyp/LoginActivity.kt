package com.example.propertyprofyp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        dbHelper = DBHelper(this)
        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPassword)
        val registerText = findViewById<TextView>(R.id.notYetRegister)
        val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)

        loginButton.setOnClickListener {
            Log.d("LoginActivity", "Login button clicked")

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        forgotPasswordText.setOnClickListener {
            val userEmail = emailEditText.text.toString()
            if (userEmail.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT)
                                .show()

                            // Clear user entry from local database
                            dbHelper.removeUser(userEmail)
                        } else {
                            Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            } else {
                Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show()
            }
        }

        passwordToggle.setOnClickListener {
            // Toggle the password visibility
            if (passwordEditText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show the password
                passwordEditText.inputType =
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_eye_open) // Change to your open eye icon
            } else {
                // Hide the password
                passwordEditText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_eye_closed) // Change to your closed eye icon
            }

            // Move the cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        registerText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Toast.makeText(this, "Login successful", Toast.LENGTH_LONG).show()

                    getUserRole { isStaff ->
                        runOnUiThread {
                            val intent = if (isStaff) {
                                Intent(this, StaffDashboardActivity::class.java)
                            } else {
                                Intent(this, ViewPropertyActivity::class.java)
                            }

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        }
                    }
                } else if (dbHelper.checkUser(email, password)) {
                    // Proceed with local login
                    navigateToDashboard()
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToDashboard() {
        getUserRole { isStaff ->
            runOnUiThread {
                val intent = if (isStaff) {
                    Intent(this, StaffDashboardActivity::class.java)
                } else {
                    Intent(this, ViewPropertyActivity::class.java)
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
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
