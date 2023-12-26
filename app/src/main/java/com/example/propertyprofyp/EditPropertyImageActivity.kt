package com.example.propertyprofyp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditPropertyImageActivity : AppCompatActivity() {

    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var addImageButton: Button
    private lateinit var updateCoverImageButton: Button
    private lateinit var coverImageView: ImageView
    private lateinit var deleteCoverIcon: ImageView
    private lateinit var imagesAdapter: ImagesAdapter
    private var propertyId: String? = null
    private var isCoverImageUpdate = false
    private val pickImageRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_property_images)

        propertyId = intent.getStringExtra("propertyId") ?: return finish()

        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        addImageButton = findViewById(R.id.addImageButton)
        updateCoverImageButton = findViewById(R.id.updateCoverImageButton)
        coverImageView = findViewById(R.id.coverImageView)
        deleteCoverIcon = findViewById(R.id.deleteCoverIcon)

        addImageButton.setOnClickListener {
            isCoverImageUpdate = false
            openImageSelector()
        }

        updateCoverImageButton.setOnClickListener {
            isCoverImageUpdate = true
            openImageSelector()
        }

        deleteCoverIcon.setOnClickListener {
            deleteCoverImage()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        fetchAndDisplayImages()
        fetchCoverImage()
    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), pickImageRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequestCode && resultCode == Activity.RESULT_OK) {
            if (data?.clipData != null) {
                // Multiple images selected
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    if (isCoverImageUpdate) {
                        uploadCoverImageToFirebase(imageUri)
                        break // Only one image can be set as cover
                    } else {
                        uploadImageToFirebase(imageUri)
                    }
                }
            } else if (data?.data != null) {
                // Single image selected
                data.data?.let { uri ->
                    if (isCoverImageUpdate) {
                        uploadCoverImageToFirebase(uri)
                    } else {
                        uploadImageToFirebase(uri)
                    }
                }
            }
        }
    }

    private fun uploadCoverImageToFirebase(imageUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().getReference("property_images/${propertyId}/coverImage")
        storageReference.putFile(imageUri).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                updateCoverImageInDatabase(downloadUrl.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload cover image: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCoverImageInDatabase(imageUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("coverImage")

        databaseReference.setValue(imageUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Cover image updated successfully", Toast.LENGTH_SHORT).show()
                fetchCoverImage()
            } else {
                Toast.makeText(this, "Failed to update cover image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().getReference("property_images/${propertyId}/${UUID.randomUUID()}")
        storageReference.putFile(imageUri).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                addImageToDatabase(downloadUrl.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addImageToDatabase(imageUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("images")

        val imageKey = databaseReference.push().key ?: return
        databaseReference.child(imageKey).setValue(imageUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Image added successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayImages() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to add image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAndDisplayImages() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("images")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val images = snapshot.value as? Map<String, String> ?: return
                setupRecyclerView(images)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }

    private fun setupRecyclerView(images: Map<String, String>) {
        imagesAdapter = ImagesAdapter(images) { imageKey ->
            deleteImage(imageKey)
        }
        imagesRecyclerView.layoutManager = GridLayoutManager(this, 4) // 4 items per row
        imagesRecyclerView.adapter = imagesAdapter
    }

    private fun deleteImage(imageKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("images")
            .child(imageKey)

        databaseReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayImages() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to delete image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCoverImage() {
        val coverImageRef = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("coverImage")

        coverImageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val coverImageUrl = snapshot.value as? String
                coverImageUrl?.let { url ->
                    Glide.with(this@EditPropertyImageActivity).load(url).into(coverImageView)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun deleteCoverImage() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("coverImage")

        databaseReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Cover image deleted successfully", Toast.LENGTH_SHORT).show()
                coverImageView.setImageResource(0) // Remove image from ImageView
            } else {
                Toast.makeText(this, "Failed to delete cover image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
