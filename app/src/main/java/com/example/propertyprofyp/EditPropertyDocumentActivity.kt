package com.example.propertyprofyp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class EditPropertyDocumentActivity : AppCompatActivity() {

    private lateinit var documentsRecyclerView: RecyclerView
    private lateinit var addDocumentButton: Button
    private lateinit var documentsAdapter: DocumentsAdapter
    private var propertyId: String? = null

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_property_documents)

        propertyId = intent.getStringExtra("propertyId") ?: return finish()

        documentsRecyclerView = findViewById(R.id.documentsRecyclerView)
        addDocumentButton = findViewById(R.id.addDocumentButton)

        addDocumentButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf" // Adjust this based on the file types you want to allow
            startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
        }

        fetchAndDisplayDocuments()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val fileUri = data?.data ?: return
            uploadDocumentToFirebaseStorage(fileUri)
        }
    }

    private fun uploadDocumentToFirebaseStorage(fileUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().getReference("path/to/storage")
        val documentReference = storageReference.child("documents/${fileUri.lastPathSegment}")
        documentReference.putFile(fileUri).addOnSuccessListener {
            documentReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                updateDocumentInFirebaseDatabase(downloadUrl.toString())
            }
        }.addOnFailureListener {
            // Handle upload failure
        }
    }

    private fun updateDocumentInFirebaseDatabase(documentUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("documents")
        val documentKey = databaseReference.push().key ?: return
        databaseReference.child(documentKey).setValue(documentUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Document uploaded successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayDocuments() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to upload document: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAndDisplayDocuments() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("documents")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val documents = snapshot.value as? Map<String, String> ?: return
                setupRecyclerView(documents)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun setupRecyclerView(documents: Map<String, String>) {
        documentsAdapter = DocumentsAdapter(documents) { documentKey ->
            deleteDocument(documentKey)
        }
        documentsRecyclerView.layoutManager = LinearLayoutManager(this)
        documentsRecyclerView.adapter = documentsAdapter
    }

    private fun deleteDocument(documentKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("documents")
            .child(documentKey)

        databaseReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayDocuments() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to delete document: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
