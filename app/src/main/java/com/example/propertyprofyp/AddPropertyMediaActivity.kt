package com.example.propertyprofyp

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.ArrayList
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide

class AddPropertyMediaActivity : AppCompatActivity() {
    private lateinit var btnChooseImages: Button
    private lateinit var btnChooseCoverImages: Button
    private lateinit var btnChooseVideos: Button
    private lateinit var btnSubmit: Button
    private lateinit var containerImages: LinearLayout
    private lateinit var containerCoverImages: LinearLayout
    private lateinit var containerVideos: LinearLayout
    private lateinit var progressDialog: AlertDialog
    private lateinit var progressBarDialog: ProgressBar
    private lateinit var progressText: TextView

    private val selectedImages = ArrayList<Uri>()
    private val selectedCoverImages = ArrayList<Uri>()
    private val selectedVideos = ArrayList<Uri>()

    private val imageDownloadUrls = ArrayList<String>()
    private val coverImageDownloadUrls = ArrayList<String>()
    private val videoDownloadUrls = ArrayList<String>()

    private lateinit var propertyId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_property_media)

        propertyId = intent.getStringExtra("propertyId") ?: ""

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        btnChooseImages = findViewById(R.id.btnChooseImages)
        btnChooseCoverImages = findViewById(R.id.btnChooseCoverImages)
        btnChooseVideos = findViewById(R.id.btnChooseVideos)
        btnSubmit = findViewById(R.id.btnSubmit)
        containerImages = findViewById(R.id.containerImages)
        containerCoverImages = findViewById(R.id.containerCoverImages)
        containerVideos = findViewById(R.id.containerVideos)
    }

    private fun setupListeners() {
        btnChooseImages.setOnClickListener { selectMedia("image/*", REQUEST_IMAGE) }
        btnChooseCoverImages.setOnClickListener { selectMedia("image/*", REQUEST_COVER_IMAGE) }
        btnChooseVideos.setOnClickListener { selectMedia("video/*", REQUEST_VIDEO) }
        btnSubmit.setOnClickListener { submitMedia() }
    }

    private fun selectMedia(type: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = type
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, requestCode)
    }

    private fun submitMedia() {
        Log.d("MediaUpload", "Starting media upload")

        // Validation checks
        if (selectedImages.size > 30) {
            Toast.makeText(this, "You can only upload up to 30 images.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCoverImages.size > 1) {
            Toast.makeText(this, "You can only upload up to 1 Cover image.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedVideos.size > 3) {
            Toast.makeText(this, "You can only upload up to 3 videos.", Toast.LENGTH_SHORT).show()
            return
        }

        val allMedia = selectedImages + selectedCoverImages + selectedVideos

        showProgressDialog()
        progressBarDialog.max = selectedImages.size + selectedCoverImages.size + selectedVideos.size
        progressBarDialog.progress = 0

        for (uri in allMedia) {
            val mediaType = when {
                selectedImages.contains(uri) -> "images"
                selectedVideos.contains(uri) -> "videos"
                else -> "coverImage"
            }

            val storageRef = FirebaseStorage.getInstance().reference.child("$propertyId/$mediaType/${uri.lastPathSegment}")
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    when (mediaType) {
                        "images" -> imageDownloadUrls.add(downloadUrl.toString())
                        "coverImage" -> coverImageDownloadUrls.add(downloadUrl.toString())
                        "videos" -> videoDownloadUrls.add(downloadUrl.toString())
                    }
                    onMediaUploadSuccess() // Call this method on successful upload
                }
            }.addOnFailureListener {

            }
        }

        Toast.makeText(this, "Uploading media, please wait...", Toast.LENGTH_LONG).show()
    }

    private fun showProgressDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.progress_dialog, null)
        builder.setView(dialogView)
        builder.setCancelable(false) // Set to false if you want to prevent dismissing the dialog

        progressBarDialog = dialogView.findViewById(R.id.progressBarDialog)
        progressText = dialogView.findViewById(R.id.progressText)

        progressDialog = builder.create()
        progressDialog.show()
    }

    private fun onMediaUploadSuccess() {
        val currentProgress = progressBarDialog.progress + 1
        progressBarDialog.progress = currentProgress
        progressText.text = "Uploading $currentProgress/${progressBarDialog.max}"

        Log.d("MediaUpload", "Current Progress: $currentProgress")
        Log.d("MediaUpload", "Selected Images: ${selectedImages.size}, Uploaded Images: ${imageDownloadUrls.size}")
        Log.d("MediaUpload", "Selected Cover Images: ${selectedCoverImages.size}, Uploaded Cover Images: ${coverImageDownloadUrls.size}")
        Log.d("MediaUpload", "Selected Videos: ${selectedVideos.size}, Uploaded Videos: ${videoDownloadUrls.size}")

        checkAllUploadsCompleted()
    }


    private fun updateFirebaseDatabase() {
        Log.d("MediaUpload", "Preparing to update database with media URLs")

        val propertyRef = FirebaseDatabase.getInstance().getReference("properties").child(propertyId)
        val allMediaUrls = ArrayList<String>().apply {
            addAll(imageDownloadUrls)
            addAll(coverImageDownloadUrls)
            addAll(videoDownloadUrls)
        }

        val updates = hashMapOf<String, Any>(
            "allMediaUrls" to allMediaUrls,
            "images" to imageDownloadUrls.mapIndexed { index, url -> "image$index" to url }.toMap(),
            "coverImage" to coverImageDownloadUrls.firstOrNull().orEmpty(),
            "videos" to videoDownloadUrls.mapIndexed { index, url -> "video$index" to url }.toMap()
        )

        propertyRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Property updated successfully", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update property", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAllUploadsCompleted() {
        if (imageDownloadUrls.size == selectedImages.size &&
            coverImageDownloadUrls.size == selectedCoverImages.size &&
            videoDownloadUrls.size == selectedVideos.size) {
            Log.d("MediaUpload", "All media uploaded successfully")
            updateFirebaseDatabase()
        } else {
            Log.d("MediaUpload", "Upload incomplete. Images: ${imageDownloadUrls.size}/${selectedImages.size}, " +
                    "Cover Images: ${coverImageDownloadUrls.size}/${selectedCoverImages.size}, " +
                    "Videos: ${videoDownloadUrls.size}/${selectedVideos.size}")
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, StaffDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_IMAGE -> handleMediaSelection(data, selectedImages, containerImages, "image")
                REQUEST_COVER_IMAGE -> handleMediaSelection(data, selectedCoverImages, containerCoverImages, "coverImage")
                REQUEST_VIDEO -> handleMediaSelection(data, selectedVideos, containerVideos, "video")
            }
        }
    }

    private fun isValidImageFormat(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType in listOf("image/jpeg", "image/png")
    }

    private fun isValidVideoFormat(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        if (mimeType != "video/mp4") {
            return false
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationStr?.toLongOrNull() ?: 0L
        return duration <= 3 * 60 * 1000 // 3 minutes in milliseconds
    }

    private fun handleMediaSelection(data: Intent, mediaList: ArrayList<Uri>, container: LinearLayout, mediaType: String) {
        val clipData = data.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val uri = clipData.getItemAt(i).uri
                if (mediaType == "image" && isValidImageFormat(uri) && !mediaList.contains(uri)) {
                    mediaList.add(uri)
                    addMediaView(uri, container, mediaType, mediaList)
                } else if (mediaType == "video" && isValidVideoFormat(uri) && !mediaList.contains(uri)) {
                    mediaList.add(uri)
                    addMediaView(uri, container, mediaType, mediaList)
                } else if (mediaType == "coverImage" && isValidImageFormat(uri) && selectedCoverImages.isEmpty()) {
                    selectedCoverImages.add(uri)
                    addMediaView(uri, containerCoverImages, "coverImage", selectedCoverImages)
                }
            }
        } else {
            data.data?.let { uri ->
                if (mediaType == "image" && isValidImageFormat(uri) && !mediaList.contains(uri)) {
                    mediaList.add(uri)
                    addMediaView(uri, container, mediaType, mediaList)
                } else if (mediaType == "video" && isValidVideoFormat(uri) && !mediaList.contains(uri)) {
                    mediaList.add(uri)
                    addMediaView(uri, container, mediaType, mediaList)
                } else if (mediaType == "coverImage" && isValidImageFormat(uri) && selectedCoverImages.isEmpty()) {
                    selectedCoverImages.add(uri)
                    addMediaView(uri, containerCoverImages, "coverImage", selectedCoverImages)
                }
            }
        }
    }

    private fun addMediaView(uri: Uri, container: LinearLayout, mediaType: String, mediaList: ArrayList<Uri>) {
        val view = LayoutInflater.from(this).inflate(R.layout.image_item, container, false)
        val imageView = view.findViewById<ImageView>(R.id.image)
        val deleteIcon = view.findViewById<ImageView>(R.id.deleteIcon)

        // Load the image or video thumbnail into imageView
        Glide.with(this).load(uri).into(imageView)

        deleteIcon.setOnClickListener {
            container.removeView(view)
            mediaList.remove(uri)
            if (mediaType == "coverImage") {
                selectedCoverImages.clear() // Clear the cover image list if cover image is removed
            }
        }

        container.addView(view)
    }

    companion object {
        private const val REQUEST_IMAGE = 1
        private const val REQUEST_COVER_IMAGE = 2
        private const val REQUEST_VIDEO = 3
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100
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
            .setMessage("Are you sure you want to exit? Any unsaved changes will be lost, and there will be no images or videos for this property.")
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
