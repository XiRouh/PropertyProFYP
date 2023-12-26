package com.example.propertyprofyp

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.FirebaseDatabase

class CalculateDSRActivity : AppCompatActivity() {

    private lateinit var editTextNetIncome: EditText
    private lateinit var editTextHomeFinancing: EditText
    private lateinit var editTextCarFinancing: EditText
    private lateinit var editTextCreditCard: EditText
    private lateinit var editTextPersonalFinancing: EditText
    private lateinit var editTextEducationLoan: EditText
    private lateinit var editTextOtherLoan: EditText
    private lateinit var submitButton: Button

    private var bookingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dsr_calculator)

        bookingId = intent.getStringExtra("bookingId")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        editTextNetIncome = findViewById(R.id.editTextNetIncome)
        editTextHomeFinancing = findViewById(R.id.editTexthomeFinancing)
        editTextCarFinancing = findViewById(R.id.editTextcarFinancing)
        editTextCreditCard = findViewById(R.id.creditCardPayment)
        editTextPersonalFinancing = findViewById(R.id.editTextpersonalFinancing)
        editTextEducationLoan = findViewById(R.id.editTextEducationLoan)
        editTextOtherLoan = findViewById(R.id.editTextOtherLoan)
        submitButton = findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            calculateAndSubmitDSR()
        }
    }

    private fun calculateAndSubmitDSR() {
        // Retrieve values from input fields
        val netIncome = editTextNetIncome.text.toString().toDoubleOrNull() ?: 0.0
        val totalDebt = listOf(
            editTextHomeFinancing.text.toString().toDoubleOrNull() ?: 0.0,
            editTextCarFinancing.text.toString().toDoubleOrNull() ?: 0.0,
            editTextCreditCard.text.toString().toDoubleOrNull() ?: 0.0,
            editTextPersonalFinancing.text.toString().toDoubleOrNull() ?: 0.0,
            editTextEducationLoan.text.toString().toDoubleOrNull() ?: 0.0,
            editTextOtherLoan.text.toString().toDoubleOrNull() ?: 0.0
        ).sum()

        val dsr = if (netIncome > 0) totalDebt / netIncome else 0.0

        // Update the loanDSR in the database for the given booking ID
        updateLoanDSRInDatabase(dsr)
    }

    private fun updateLoanDSRInDatabase(dsr: Double) {
        bookingId?.let { id ->
            val purchasesRef = FirebaseDatabase.getInstance().getReference("purchases")
            purchasesRef.child(id).child("loanDSR").setValue(dsr).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "DSR updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update DSR: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
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
