package com.valdi.perfectrepapp.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.valdi.perfectrepapp.R

class SessionDateAdapter(
    private val dates: MutableList<String>,  // Cambia a MutableList para poder modificar la lista
    private val onDateClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit  // Callback para eliminar
) : RecyclerView.Adapter<SessionDateAdapter.SessionDateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionDateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_date, parent, false)
        return SessionDateViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionDateViewHolder, position: Int) {
        val date = dates[position]
        holder.bind(date)
    }

    override fun getItemCount() = dates.size

    inner class SessionDateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(date: String) {
            dateTextView.text = date
            itemView.setOnClickListener { onDateClick(date) }
            deleteButton.setOnClickListener { onDeleteClick(date) }  // Llama a la función onDeleteClick
        }
    }

    fun removeDate(date: String) {
        val position = dates.indexOf(date)
        if (position != -1) {
            dates.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}

