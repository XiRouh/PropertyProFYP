package com.example.propertyprofyp

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue

class UpdatePermissionActivity : AppCompatActivity() {

    private lateinit var checkboxAddNewProperty: CheckBox
    private lateinit var checkboxUpdatePropertyDetails: CheckBox
    private lateinit var checkboxRemoveProperty: CheckBox
    private lateinit var checkboxSendAnnouncement: CheckBox
    private lateinit var checkboxViewClientWishlistReport: CheckBox
    private lateinit var checkboxUpdateStaffPermission: CheckBox
    private lateinit var checkboxDeleteStaff: CheckBox
    private lateinit var buttonUpdatePermissions: Button

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.update_permission)

        userId = intent.getStringExtra("userId")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get the logged-in user's ID
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        checkUserPermission(loggedInUserId)

        initializeViews()
        loadCurrentPermissions()
        setupUpdateButton()
    }

    private fun checkUserPermission(loggedInUserId: String?) {
        loggedInUserId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
            databaseRef.get().addOnSuccessListener { dataSnapshot ->
                // Update how permissions are retrieved from the dataSnapshot
                val permissions = dataSnapshot.getValue<Map<String, Boolean>>() ?: emptyMap()
                if (permissions["updateStaffPermission"] != true) {
                    Toast.makeText(this, "You do not have permission to update staff permissions", Toast.LENGTH_LONG).show()
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

    private fun initializeViews() {
        checkboxAddNewProperty = findViewById(R.id.checkbox_add_new_property)
        checkboxUpdatePropertyDetails = findViewById(R.id.checkbox_update_property_details)
        checkboxRemoveProperty = findViewById(R.id.checkbox_remove_property)
        checkboxSendAnnouncement = findViewById(R.id.checkbox_send_announcement)
        checkboxViewClientWishlistReport = findViewById(R.id.checkbox_view_client_wishlist_report)
        checkboxUpdateStaffPermission = findViewById(R.id.checkbox_update_staff_permission)
        checkboxDeleteStaff = findViewById(R.id.checkbox_delete_staff)
        buttonUpdatePermissions = findViewById(R.id.button_update_permissions)
    }

    private fun loadCurrentPermissions() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
        databaseRef.get().addOnSuccessListener { dataSnapshot ->
            dataSnapshot.children.forEach { child ->
                when (child.key) {
                    "addNewProperty" -> checkboxAddNewProperty.isChecked = child.getValue(Boolean::class.java) ?: false
                    "updatePropertyDetails" -> checkboxUpdatePropertyDetails.isChecked = child.getValue(Boolean::class.java) ?: false
                    "removeProperty" -> checkboxRemoveProperty.isChecked = child.getValue(Boolean::class.java) ?: false
                    "sendAnnouncement" -> checkboxSendAnnouncement.isChecked = child.getValue(Boolean::class.java) ?: false
                    "viewClientWishlistReport" -> checkboxViewClientWishlistReport.isChecked = child.getValue(Boolean::class.java) ?: false
                    "updateStaffPermission" -> checkboxUpdateStaffPermission.isChecked = child.getValue(Boolean::class.java) ?: false
                    "deleteStaff" -> checkboxDeleteStaff.isChecked = child.getValue(Boolean::class.java) ?: false
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load permissions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUpdateButton() {
        buttonUpdatePermissions.setOnClickListener {
            updatePermissions()
        }
    }

    private fun updatePermissions() {
        val permissions = hashMapOf(
            "addNewProperty" to checkboxAddNewProperty.isChecked,
            "updatePropertyDetails" to checkboxUpdatePropertyDetails.isChecked,
            "removeProperty" to checkboxRemoveProperty.isChecked,
            "sendAnnouncement" to checkboxSendAnnouncement.isChecked,
            "viewClientWishlistReport" to checkboxViewClientWishlistReport.isChecked,
            "updateStaffPermission" to checkboxUpdateStaffPermission.isChecked,
            "deleteStaff" to checkboxDeleteStaff.isChecked
        )

        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
        databaseRef.setValue(permissions)
            .addOnSuccessListener {
                Toast.makeText(this, "Permissions updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update permissions", Toast.LENGTH_SHORT).show()
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
