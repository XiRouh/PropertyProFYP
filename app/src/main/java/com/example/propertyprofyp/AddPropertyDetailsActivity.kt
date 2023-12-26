package com.example.propertyprofyp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import androidx.appcompat.widget.Toolbar

class AddPropertyDetailsActivity : AppCompatActivity() {

    private lateinit var spinnerPropertyType: Spinner
    private lateinit var spinnerTenure: Spinner
    private lateinit var spinnerArea: Spinner
    private lateinit var editTextLocation: EditText
    private lateinit var editTextBuildUp: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextProjectName: EditText
    private lateinit var editTextDeveloper: EditText
    private lateinit var editTextCompletionDate: EditText
    private lateinit var editTextMaintenanceFee: EditText
    private lateinit var editTextPackage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_property_details)

        // Initialize UI components
        spinnerPropertyType = findViewById(R.id.spinnerPropertyType)
        spinnerTenure = findViewById(R.id.spinnerTenure)
        spinnerArea = findViewById(R.id.spinnerArea)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextBuildUp = findViewById(R.id.editTextBuildUp)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextDescription = findViewById(R.id.editTextDescription)
        editTextProjectName = findViewById(R.id.editTextProjectName)
        editTextDeveloper = findViewById(R.id.editTextDeveloper)
        editTextCompletionDate = findViewById(R.id.editTextCompletionDate)
        editTextMaintenanceFee = findViewById(R.id.editTextMaintenanceFee)
        editTextPackage = findViewById(R.id.editTextPackage)
        val submitButton: Button = findViewById(R.id.submitButton)

        setupSpinner(spinnerPropertyType, R.array.propertyType)
        setupSpinner(spinnerTenure, R.array.tenureType)
        setupSpinner(spinnerArea, R.array.areaType)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get the logged-in user's ID
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        checkUserPermission(loggedInUserId)

        submitButton.setOnClickListener {
            if (validateFields()) {
                val propertyId = FirebaseDatabase.getInstance().getReference("properties").push().key ?: return@setOnClickListener
                submitPropertyDetails(
                    propertyId,
                    spinnerPropertyType.selectedItem.toString(),
                    spinnerTenure.selectedItem.toString(),
                    spinnerArea.selectedItem.toString(),
                    editTextLocation.text.toString(),
                    editTextBuildUp.text.toString().toDoubleOrNull() ?: 0.0,
                    editTextPrice.text.toString().toDoubleOrNull() ?: 0.0,
                    editTextDescription.text.toString(),
                    editTextProjectName.text.toString(),
                    editTextDeveloper.text.toString(),
                    editTextCompletionDate.text.toString(),
                    editTextMaintenanceFee.text.toString().toDoubleOrNull() ?: 0.0,
                    editTextPackage.text.toString()
                )
            }
        }
    }

    private fun checkUserPermission(loggedInUserId: String?) {
        loggedInUserId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
            databaseRef.get().addOnSuccessListener { dataSnapshot ->
                // Update how permissions are retrieved from the dataSnapshot
                val permissions = dataSnapshot.getValue<Map<String, Boolean>>() ?: emptyMap()
                if (permissions["addNewProperty"] != true) {
                    Toast.makeText(this, "You do not have permission to add new property", Toast.LENGTH_LONG).show()
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

    private fun setupSpinner(spinner: Spinner, arrayId: Int) {
        ArrayAdapter.createFromResource(
            this,
            arrayId,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun validateFields(): Boolean {
        if (editTextLocation.text.toString().isEmpty() ||
            editTextBuildUp.text.toString().isEmpty() ||
            editTextPrice.text.toString().isEmpty() ||
            editTextDescription.text.toString().isEmpty() ||
            editTextProjectName.text.toString().isEmpty() ||
            editTextDeveloper.text.toString().isEmpty() ||
            editTextCompletionDate.text.toString().isEmpty() ||
            editTextMaintenanceFee.text.toString().isEmpty() ||
            editTextPackage.text.toString().isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun submitPropertyDetails(
        id: String,
        propertyType: String,
        tenure: String,
        area: String,
        location: String,
        buildUp: Double,
        price: Double,
        description: String,
        projectName: String,
        developer: String,
        completionDate: String,
        maintenanceFee: Double,
        packageInfo: String
    ) {
        val newProperty = Property(
            id = id,
            propertyType = propertyType,
            tenure = tenure,
            area = area,
            location = location,
            buildUp = buildUp,
            price = price,
            description = description,
            projectName = projectName,
            developer = developer,
            completionDate = completionDate,
            maintenanceFee = maintenanceFee,
            packageInfo = packageInfo
        )

        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.child(id).setValue(newProperty).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Property added successfully", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, AddPropertyMediaActivity::class.java)
                intent.putExtra("propertyId", id)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to add property: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
