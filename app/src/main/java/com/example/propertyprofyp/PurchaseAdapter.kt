package com.example.propertyprofyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PurchaseAdapter(
    private var purchases: MutableList<Purchase>,
    private val onPurchaseInteraction: (Purchase, InteractionType) -> Unit,
    private val getUserById: (String, (String) -> Unit) -> Unit
) : RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder>() {

    class PurchaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookingId: TextView = view.findViewById(R.id.bookingId)
        val userName: TextView = view.findViewById(R.id.userName)
        val staffName: TextView = view.findViewById(R.id.staffName)
        val projectName: TextView = view.findViewById(R.id.bookingProjectName)
        val projectId: TextView = view.findViewById(R.id.projectId)
        val price: TextView = view.findViewById(R.id.price)
        val expectedCommission: TextView = view.findViewById(R.id.expectedCommission)
        val loanDSR: TextView = view.findViewById(R.id.loanDSR)
        val calculateLoanDSRBtn: Button = view.findViewById(R.id.calculateLoanDSRBtn)
        val cancelBtn: Button = view.findViewById(R.id.cancelBtn)
    }

    enum class InteractionType {
        CALCULATE_LOAN_DSR, CANCEL_PURCHASE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.purchases_card_design, parent, false)
        return PurchaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        val purchase = purchases[position]

        // Fetch and set the user name
        getUserById(purchase.userId) { username ->
            holder.userName.text = username
        }

        // Fetch and set the staff name
        getUserById(purchase.staffId) { staffName ->
            holder.staffName.text = staffName
        }

        holder.bookingId.text = purchase.bookingId
        holder.projectName.text = purchase.projectName
        holder.projectId.text = purchase.projectId
        holder.price.text = purchase.price.toString()
        holder.loanDSR.text = (purchase.loanDSR*100).toString() + "%"

        val commissionRate = 0.05  // 5% commission rate
        val expectedCommission = purchase.price * commissionRate
        holder.expectedCommission.text = "${String.format("%.2f", expectedCommission)}"

        holder.calculateLoanDSRBtn.setOnClickListener {
            onPurchaseInteraction(purchase, InteractionType.CALCULATE_LOAN_DSR)
        }
        holder.cancelBtn.setOnClickListener {
            onPurchaseInteraction(purchase, InteractionType.CANCEL_PURCHASE)
        }
    }

    override fun getItemCount(): Int = purchases.size
}
