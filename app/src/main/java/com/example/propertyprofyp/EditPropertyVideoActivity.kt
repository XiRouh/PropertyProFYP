package com.example.propertyprofyp

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class EditPropertyVideoActivity : AppCompatActivity() {
    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var addVideoButton: Button
    private lateinit var videosAdapter: VideosAdapter
    private var propertyId: String? = null

    companion object {
        private const val REQUEST_VIDEO_PICK = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_property_videos)

        propertyId = intent.getStringExtra("propertyId") ?: return finish()

        videosRecyclerView = findViewById(R.id.videosRecyclerView)
        addVideoButton = findViewById(R.id.addVideoButton)

        val isViewOnly = intent.getBooleanExtra("isViewOnly", false)
        Log.d("VideoActivity", "Is View Only: $isViewOnly")
        if (isViewOnly) {
            addVideoButton.visibility = View.GONE
        }

        addVideoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_VIDEO_PICK)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        fetchAndDisplayVideos()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK) {
            data?.data?.let { videoUri ->
                uploadVideoToFirebase(videoUri)
            }
        }
    }

    private fun uploadVideoToFirebase(videoUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().reference
        val videoRef = storageReference.child("path/to/videos/${videoUri.lastPathSegment}")

        val uploadTask = videoRef.putFile(videoUri)
        uploadTask.addOnSuccessListener {
            videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                saveVideoUrlToDatabase(downloadUrl.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveVideoUrlToDatabase(videoUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties/$propertyId/videos")
        val videoKey = databaseReference.push().key ?: return

        databaseReference.child(videoKey).setValue(videoUrl).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Video added successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayVideos()
            } else {
                Toast.makeText(this, "Failed to add video: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAndDisplayVideos() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("videos")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val videos = snapshot.value as? Map<String, String> ?: return
                setupRecyclerView(videos)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditPropertyVideoActivity, "Error fetching videos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView(videos: Map<String, String>) {
        val isViewOnly = intent.getBooleanExtra("isViewOnly", false)
        videosAdapter = VideosAdapter(videos, { videoKey ->
            if (!isViewOnly) deleteVideo(videoKey)
        }, { videoUrl ->
            playVideo(videoUrl)
        }, isViewOnly) // Pass isViewOnly directly here

        videosRecyclerView.layoutManager = LinearLayoutManager(this)
        videosRecyclerView.adapter = videosAdapter
    }

    private fun playVideo(videoUrl: String) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("videoUrl", videoUrl)
        startActivity(intent)
    }

    private fun deleteVideo(videoKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("properties")
            .child(propertyId!!)
            .child("videos")
            .child(videoKey)

        databaseReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayVideos() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to delete video: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
