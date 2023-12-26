package com.example.propertyprofyp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditPropertyDetailsActivity : AppCompatActivity() {

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
    private lateinit var submitButton: Button
    private lateinit var imageBar: TextView
    private lateinit var videoBar: TextView
    private var propertyId: String? = null
    private var originalProperty: Property? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_property_details)

        propertyId = intent.getStringExtra("propertyId")
        if (propertyId == null) {
            // Handle null property ID
            finish()
            return
        }

        // Get the logged-in user's ID
        val loggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        checkUserPermission(loggedInUserId)

        initializeViews()
        setupSpinners()
        fetchPropertyDetails(propertyId!!)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun checkUserPermission(loggedInUserId: String?) {
        loggedInUserId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/permissions")
            databaseRef.get().addOnSuccessListener { dataSnapshot ->
                // Update how permissions are retrieved from the dataSnapshot
                val permissions = dataSnapshot.getValue<Map<String, Boolean>>() ?: emptyMap()
                if (permissions["updatePropertyDetails"] != true) {
                    Toast.makeText(this, "You do not have permission to update property details", Toast.LENGTH_LONG).show()
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
        submitButton = findViewById(R.id.submitButton)

        submitButton.setOnClickListener { updatePropertyDetails() }

        imageBar = findViewById(R.id.imageBar)
        imageBar.setOnClickListener {
            val intent = Intent(this, EditPropertyImageActivity::class.java)
            intent.putExtra("propertyId", propertyId) // assuming 'propertyId' holds the ID of the current property
            startActivity(intent)
        }

        videoBar = findViewById(R.id.videoBar)
        videoBar.setOnClickListener {
            val intent = Intent(this, EditPropertyVideoActivity::class.java)
            intent.putExtra("propertyId", propertyId) // assuming 'propertyId' holds the ID of the current property
            startActivity(intent)
        }
    }

    private fun setupSpinners() {
        // Property Type Spinner Setup
        ArrayAdapter.createFromResource(
            this,
            R.array.propertyType, // Replace with your array resource for property types
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPropertyType.adapter = adapter
        }

        // Tenure Spinner Setup
        ArrayAdapter.createFromResource(
            this,
            R.array.tenureType, // Replace with your array resource for tenure types
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTenure.adapter = adapter
        }

        // Area Spinner Setup
        ArrayAdapter.createFromResource(
            this,
            R.array.areaType,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerArea.adapter = adapter
        }
    }


    private fun fetchPropertyDetails(propertyId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.child(propertyId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val property = snapshot.getValue(Property::class.java)
                property?.let { displayPropertyData(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun displayPropertyData(property: Property) {
        originalProperty = property

        // Displaying current data in TextViews
        findViewById<TextView>(R.id.propertyType).text = property.propertyType
        findViewById<TextView>(R.id.tenureType).text = property.tenure
        findViewById<TextView>(R.id.areaType).text = property.area
        findViewById<TextView>(R.id.location).text = property.location
        findViewById<TextView>(R.id.buildUp).text = property.buildUp.toString()
        findViewById<TextView>(R.id.price).text = property.price.toString()
        findViewById<TextView>(R.id.description).text = property.description
        findViewById<TextView>(R.id.projectName).text = property.projectName
        findViewById<TextView>(R.id.developer).text = property.developer
        findViewById<TextView>(R.id.completionDate).text = property.completionDate
        findViewById<TextView>(R.id.maintenanceFee).text = property.maintenanceFee.toString()
        findViewById<TextView>(R.id.packages).text = property.packageInfo

        // Setting default values in EditTexts
        editTextLocation.setText(property.location)
        editTextBuildUp.setText(property.buildUp.toString())
        editTextPrice.setText(property.price.toString())
        editTextDescription.setText(property.description)
        editTextProjectName.setText(property.projectName)
        editTextDeveloper.setText(property.developer)
        editTextCompletionDate.setText(property.completionDate)
        editTextMaintenanceFee.setText(property.maintenanceFee.toString())
        editTextPackage.setText(property.packageInfo)

        // Setting the default selected item in Spinners based on current data
        setSpinnerSelection(spinnerPropertyType, R.array.propertyType, property.propertyType)
        setSpinnerSelection(spinnerTenure, R.array.tenureType, property.tenure)
        setSpinnerSelection(spinnerArea, R.array.areaType, property.area)
    }

    private fun setSpinnerSelection(spinner: Spinner, arrayId: Int, currentValue: String) {
        val array = resources.getStringArray(arrayId)
        val index = array.indexOf(currentValue)
        if (index >= 0) {
            spinner.setSelection(index)
        }
    }


    private fun updatePropertyDetails() {
        val updatedProperty = originalProperty?.copy(
            propertyType = if (spinnerPropertyType.selectedItem.toString().isNotBlank()) spinnerPropertyType.selectedItem.toString() else originalProperty!!.propertyType,
            tenure = if (spinnerTenure.selectedItem.toString().isNotBlank()) spinnerTenure.selectedItem.toString() else originalProperty!!.tenure,
            area = if (spinnerArea.selectedItem.toString().isNotBlank()) spinnerArea.selectedItem.toString() else originalProperty!!.area,
            location = if (editTextLocation.text.isNotBlank()) editTextLocation.text.toString() else originalProperty!!.location,
            buildUp = if (editTextBuildUp.text.isNotBlank()) editTextBuildUp.text.toString().toDouble() else originalProperty!!.buildUp,
            price = if (editTextPrice.text.isNotBlank()) editTextPrice.text.toString().toDouble() else originalProperty!!.price,
            description = if (editTextDescription.text.isNotBlank()) editTextDescription.text.toString() else originalProperty!!.description,
            projectName = if (editTextProjectName.text.isNotBlank()) editTextProjectName.text.toString() else originalProperty!!.projectName,
            developer = if (editTextDeveloper.text.isNotBlank()) editTextDeveloper.text.toString() else originalProperty!!.developer,
            completionDate = if (editTextCompletionDate.text.isNotBlank()) editTextCompletionDate.text.toString() else originalProperty!!.completionDate,
            maintenanceFee = if (editTextMaintenanceFee.text.isNotBlank()) editTextMaintenanceFee.text.toString().toDouble() else originalProperty!!.maintenanceFee,
            packageInfo = if (editTextPackage.text.isNotBlank()) editTextPackage.text.toString() else originalProperty!!.packageInfo
        )

        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
        databaseReference.child(propertyId!!).setValue(updatedProperty)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Property updated successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ViewPropertyActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update property: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
