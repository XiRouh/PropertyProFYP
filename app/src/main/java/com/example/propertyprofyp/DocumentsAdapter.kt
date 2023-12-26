package com.example.propertyprofyp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DocumentsAdapter(
    private val documents: Map<String, String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val documentName: TextView = view.findViewById(R.id.documentName)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    private val documentKeys = documents.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.document_item, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val documentKey = documentKeys[position]
        holder.documentName.text = documents[documentKey]
        holder.deleteButton.setOnClickListener { onDeleteClick(documentKey) }
    }

    override fun getItemCount() = documents.size
}
