package com.example.propertyprofyp

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_details)

        videoView = findViewById(R.id.propertyVideo)
        val videoUrl = intent.getStringExtra("videoUrl")

        videoView.setVideoURI(Uri.parse(videoUrl))
        videoView.start()
    }
}
