package com.example.propertyprofyp

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide

class MediaPagerAdapter(private val mediaUrls: List<String>) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val url = mediaUrls[position]
        return if (url.contains("/videos/")) { // Check if the URL contains '/videos/'
            createVideoView(url, container)
        } else {
            createImageView(url, container)
        }
    }

    private fun createImageView(url: String, container: ViewGroup): ImageView {
        val imageView = ImageView(container.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(url).into(this)
        }
        container.addView(imageView)
        return imageView
    }

    private fun createVideoView(url: String, container: ViewGroup): VideoView {
        val videoView = VideoView(container.context).apply {
            setVideoURI(Uri.parse(url))
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                start() // Autoplay
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(container.context, "Error playing video", Toast.LENGTH_SHORT).show()
                true
            }
        }
        container.addView(videoView)
        return videoView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return mediaUrls.size
    }
}
