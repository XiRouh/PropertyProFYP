package com.example.propertyprofyp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private lateinit var dbHelper: DBHelper

    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        dbHelper = DBHelper(this)

        val changeProfileImageButton = findViewById<ImageView>(R.id.changeProfileImageButton)
        changeProfileImageButton.setOnClickListener {
            openImagePicker()
        }

        val alreadyRegistered = findViewById<TextView>(R.id.alreadyRegistered)
        alreadyRegistered.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)
        val passwordEditText = findViewById<EditText>(R.id.password)
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

        val passwordToggle2 = findViewById<ImageView>(R.id.passwordToggle2)
        val confirmPassword = findViewById<EditText>(R.id.confirmPassword)
        passwordToggle2.setOnClickListener {
            // Toggle the password visibility
            if (confirmPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show the password
                confirmPassword.inputType =
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle2.setImageResource(R.drawable.ic_eye_open) // Change to your open eye icon
            } else {
                // Hide the password
                confirmPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle2.setImageResource(R.drawable.ic_eye_closed) // Change to your closed eye icon
            }

            // Move the cursor to the end of the text
            confirmPassword.setSelection(passwordEditText.text.length)
        }

        val signUpButton = findViewById<Button>(R.id.signUpBtn)
        signUpButton.setOnClickListener {
            registerUser()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data

            // Log the URI for debugging
            Log.d("SignUpActivity", "Selected image URI: $selectedImageUri")

            val profileImageView = findViewById<ImageView>(R.id.profileImageView)
            profileImageView.setImageURI(selectedImageUri)
        }
    }

    private fun registerUser() {
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val usernameEditText = findViewById<EditText>(R.id.username)
        val userTypeSpinner = findViewById<Spinner>(R.id.userType)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPassword)

        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val userType = userTypeSpinner.selectedItem.toString()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPasswordValid(password)) {
            Toast.makeText(
                this,
                "Password must be at least 7 characters long and contain at least 1 special character, 1 capital letter, 1 small letter, and 1 digit",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (confirmPassword != password) {
            Toast.makeText(this, "Confirm password need to match with password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save user to SQLite database
                    dbHelper.addUser(email, password)

                    saveUserToDatabase(email, username, userType)
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=/?<>,.';:|~])(?=\\S+\$).{7,}\$"
        return password.matches(passwordRegex.toRegex())
    }

    private fun saveUserToDatabase(email: String, username: String, userType: String) {
        val userId = auth.currentUser?.uid ?: ""

        // Remove the image upload logic from here
        val user = User(userId, email, username, userType, null)
        database.reference.child("users").child(userId).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userId.let { updateToken(it) }
                    // After saving the user data, upload the profile image
                    uploadProfileImage(userId, selectedImageUri)
                    Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Database error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("token")
                .setValue(token)
        }
    }

    private fun uploadProfileImage(userId: String?, imageUri: Uri?) {
        val storageRef = FirebaseStorage.getInstance().reference
        val profileImageRef = storageRef.child("profile_images").child(userId ?: "")

        if (imageUri != null) {
            val uploadTask = profileImageRef.putFile(imageUri)
            uploadTask.addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                    val database = FirebaseDatabase.getInstance()
                    val usersRef = database.getReference("users")
                    userId?.let {
                        usersRef.child(it).child("profileImageUrl").setValue(uri.toString())
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload profile image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("users")
            userId?.let {
                usersRef.child(it).child("profileImageUrl").setValue(null)
            }
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
