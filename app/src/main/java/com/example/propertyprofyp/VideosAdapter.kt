package com.example.propertyprofyp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideosAdapter(
    private val videos: Map<String, String>,
    private val onDeleteClick: (String) -> Unit,
    private val onVideoClick: (String) -> Unit,
    private var isViewOnly: Boolean
) : RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoThumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val deleteButton: ImageView = view.findViewById(R.id.deleteVideoButton) // Changed to ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val (key, url) = videos.entries.elementAt(position)

        // Load video thumbnail using Glide or another library of your choice
        // This example uses a placeholder image, replace with actual video thumbnails
        Glide.with(holder.videoThumbnail.context)
            .load(url) // You might need to find a way to load thumbnails from video URLs
            .placeholder(R.drawable.play_icon) // Replace with a real placeholder
            .into(holder.videoThumbnail)

        holder.videoThumbnail.setOnClickListener {
            onVideoClick(url)
        }
        Log.d("VideosAdapter", "Is View Only in Adapter: $isViewOnly")

        holder.deleteButton.visibility = if (isViewOnly) View.GONE else View.VISIBLE

        holder.deleteButton.setOnClickListener {
            onDeleteClick(key)
        }
    }

    override fun getItemCount() = videos.size
}
