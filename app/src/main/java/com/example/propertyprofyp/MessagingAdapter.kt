package com.example.propertyprofyp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.propertyprofyp.BotConstants.RECEIVE_ID
import com.example.propertyprofyp.BotConstants.SEND_ID
import com.example.propertyprofyp.databinding.HomeCardDesignBinding
import com.example.propertyprofyp.databinding.MessageItemBinding

class MessagingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var messagesList = mutableListOf<Message>()

    override fun getItemViewType(position: Int): Int {
        return when {
            messagesList[position].id == BotConstants.PROPERTY_ID -> R.layout.home_card_design
            else -> R.layout.message_item  // Your existing message item layout
        }
    }

    fun insertMessage(message: Message) {
        messagesList.add(message)
        notifyItemInserted(messagesList.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.home_card_design -> {
                val binding = HomeCardDesignBinding.inflate(inflater, parent, false)
                PropertyViewHolder(binding)
            }
            else -> {
                val binding = MessageItemBinding.inflate(inflater, parent, false)
                MessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messagesList[position]
        when (holder) {
            is PropertyViewHolder -> {
                currentMessage.property?.let { holder.bind(it) }
            }
            is MessageViewHolder -> {
                holder.bind(currentMessage)
            }
        }
    }

    override fun getItemCount(): Int = messagesList.size

    inner class PropertyViewHolder(private val binding: HomeCardDesignBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(property: Property) {
            val coverImageUrl = property.coverImage
            Glide.with(binding.propertyImg.context).load(coverImageUrl).into(binding.propertyImg)
            binding.projectName.text = property.projectName
            binding.propertyType.text = property.propertyType
            binding.price.text = property.price.toString()
            binding.area.text = property.area

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, UserPropertyDetailsActivity::class.java)
                intent.putExtra("propertyId", property.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    inner class MessageViewHolder(private val binding: MessageItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.id == SEND_ID) {
                binding.tvMessage.text = message.message
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvBotMessage.visibility = View.GONE
            } else if (message.id == RECEIVE_ID) {
                binding.tvBotMessage.text = message.message
                binding.tvBotMessage.visibility = View.VISIBLE
                binding.tvMessage.visibility = View.GONE
            }
        }
    }
}

