package com.example.propertyprofyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.propertyprofyp.R

class BookingAdapter(
    private val bookings: List<Booking>,
    private val onBookingClick: (Booking) -> Unit,
    private val onUpdateStatusClick: (Booking) -> Unit,
    private val onCancelBookingClick: (Booking) -> Unit,
    private val isStaff: Boolean, // Pass the user role
    private val getUserById: (String, (String) -> Unit) -> Unit // Function to fetch username by ID
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookingId: TextView = view.findViewById(R.id.bookingId)
        val userName: TextView = view.findViewById(R.id.userName)
        val staffName: TextView = view.findViewById(R.id.staffName)
        val bookingDateTime: TextView = view.findViewById(R.id.bookingDateTime)
        val bookingProject: TextView = view.findViewById(R.id.bookingProject)
        val bookingAddress: TextView = view.findViewById(R.id.bookingAddress)
        val bookingStatus: TextView = view.findViewById(R.id.bookingStatus)
        val changeAppointmentBtn: Button = view.findViewById(R.id.changeAppointmentBtn)
        val updateStatusBtn: Button = view.findViewById(R.id.updateStatusBtn)
        val cancelBtn: Button = view.findViewById(R.id.cancelBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.booking_card_design, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.itemView.setOnClickListener { onBookingClick(booking) }

        holder.bookingId.text = booking.id
        holder.bookingAddress.text = booking.address
        holder.bookingProject.text = booking.projectName
        holder.bookingStatus.text = booking.status
        holder.bookingDateTime.text = booking.date + " " + booking.time
        getUserById(booking.staffId) { username ->
            holder.staffName.text = username
        }
        // Fetch and set the user name
        getUserById(booking.userId) { username ->
            holder.userName.text = username
        }

        holder.changeAppointmentBtn.visibility = if (isStaff) View.GONE else View.VISIBLE
        holder.updateStatusBtn.visibility = if (isStaff) View.VISIBLE else View.GONE

        holder.changeAppointmentBtn.setOnClickListener {
            if (booking.status == "Pending") {
                onBookingClick(booking)
            } else {
                Toast.makeText(holder.itemView.context, "Only pending appointments can be changed.", Toast.LENGTH_SHORT).show()
            }
        }

        holder.updateStatusBtn.setOnClickListener {
            if (booking.status != "Done") {
                onUpdateStatusClick(booking)
            }
        }

        holder.cancelBtn.setOnClickListener {
            onCancelBookingClick(booking)
        }
    }

    override fun getItemCount() = bookings.size
}

