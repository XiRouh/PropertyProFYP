package com.example.propertyprofyp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var emailEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var nameProfileTextView2: TextView
    private lateinit var passwordEditText: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var editProfileImageBtn: ImageView
    private lateinit var editEmailBtn: ImageView
    private lateinit var editNameBtn: ImageView
    private lateinit var editPasswordBtn: ImageView
    private lateinit var searchBotBtn: Button
    private lateinit var logoutBtn: Button
    private lateinit var deleteProfileBtn: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile)

        val toolbarNotificationIcon = findViewById<ImageView>(R.id.toolbarNotificationIcon)
        toolbarNotificationIcon.setOnClickListener {
            val intent = Intent(this, ViewNotificationActivity::class.java)
            startActivity(intent)
        }

        // Initialize Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set up the bottom navigation
        setupBottomNavigation(UserProfileActivity::class.java)

        emailEditText = findViewById(R.id.emailProfileTextView)
        nameEditText = findViewById(R.id.nameProfileTextView)
        passwordEditText = findViewById(R.id.passwordProfileTextView)
        nameProfileTextView2 = findViewById(R.id.nameProfileTextView2)
        profileImageView = findViewById(R.id.profileImageView)
        editProfileImageBtn = findViewById(R.id.editProfileImageBtn)
        editEmailBtn = findViewById(R.id.editEmailBtn)
        editNameBtn = findViewById(R.id.editNameBtn)
        editPasswordBtn = findViewById(R.id.editPasswordBtn)
        searchBotBtn = findViewById(R.id.searchBotBtn)
        logoutBtn = findViewById(R.id.logoutBtn)
        deleteProfileBtn = findViewById(R.id.deleteProfileBtn)

        emailEditText.tag = "email"
        nameEditText.tag = "username"

        searchBotBtn.setOnClickListener {
            val intent = Intent(this, SearchBotActivity::class.java)
            startActivity(intent)
        }

        loadUserData()
        setupEditableFields()
        setupProfileImageEditing()
        setupPasswordEditing()
        setupLogoutAndDelete()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        databaseRef.get().addOnSuccessListener { dataSnapshot ->
            val user = dataSnapshot.getValue(User::class.java)
            user?.let {
                emailEditText.setText(it.email)
                nameEditText.setText(it.username)
                nameProfileTextView2.setText("Hey! " + it.username)
                passwordEditText.setText("******") // Masked password

                Glide.with(this)
                    .load(it.profileImageUrl)
                    .placeholder(R.drawable.default_profile_image)
                    .circleCrop() // Make the image round
                    .into(profileImageView)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEditableFields() {
        editEmailBtn.setOnClickListener {
            Log.d("UserProfileActivity", "Edit email button clicked")
            toggleEditing(emailEditText)
        }
        editNameBtn.setOnClickListener {
            Log.d("UserProfileActivity", "Edit name button clicked")
            toggleEditing(nameEditText)
        }
    }

    private fun toggleEditing(editText: EditText) {
        if (editText.isFocusable) {
            Log.d("UserProfileActivity", "Field unfocused: " + editText.tag.toString())
            editText.isFocusable = false
            editText.isFocusableInTouchMode = false
            updateUserData(editText.tag.toString(), editText.text.toString())
        } else {
            Log.d("UserProfileActivity", "Field focused: " + editText.tag.toString())
            editText.isFocusableInTouchMode = true
            editText.isFocusable = true
            editText.requestFocus()
        }
    }

    private fun updateUserData(field: String, value: String) {
        val userId = auth.currentUser?.uid ?: return
        if (value.isNotEmpty()) {
            Log.d("UserProfileActivity", "Updating $field to $value")
            FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child(field)
                .setValue(value)
                .addOnSuccessListener {
                    Log.d("UserProfileActivity", "$field updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("UserProfileActivity", "Failed to update $field: ${e.message}")
                    Toast.makeText(this, "Failed to update $field: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("UserProfileActivity", "$field update skipped as value is empty")
        }
    }

    private fun setupProfileImageEditing() {
        editProfileImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: return
            uploadImageToFirebase(imageUri)
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId")
        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                updateUserData("profileImageUrl", uri.toString())
                Glide.with(this).load(uri).into(profileImageView)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPasswordEditing() {
        editPasswordBtn.setOnClickListener {
            val userEmail = emailEditText.text.toString()
            if (userEmail.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLogoutAndDelete() {
        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        deleteProfileBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteUserProfile()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteUserProfile() {
        val user = auth.currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                deleteUserFromDatabase(user.uid)
            } else {
                Toast.makeText(this, "Failed to delete user account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteUserFromDatabase(userId: String) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(userId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "User account deleted", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove user data from database", Toast.LENGTH_SHORT).show()
            }
    }
}
