package com.valdi.perfectrepapp.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.valdi.perfectrepapp.R

class ErrorAdapter(private val errorList: List<ErrorItem>) : RecyclerView.Adapter<ErrorAdapter.ErrorViewHolder>() {

    class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val errorNameTextView: TextView = itemView.findViewById(R.id.errorNameTextView)
        val errorCountTextView: TextView = itemView.findViewById(R.id.errorCountTextView)
        val errorRecommendationTextView: TextView = itemView.findViewById(R.id.errorRecommendationTextView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_error, parent, false)
        return ErrorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ErrorViewHolder, position: Int) {
        val errorItem = errorList[position]
        holder.errorNameTextView.text = errorItem.errorName

        // Modificamos el texto para que sea "vez" o "veces" según el conteo
        val countText = "${errorItem.errorCount} ${if (errorItem.errorCount == 1) "vez" else "veces"}"
        holder.errorCountTextView.text = countText

        // Mostrar la recomendación si existe
        if (errorItem.recommendation.isNotEmpty()) {
            holder.errorRecommendationTextView.text = errorItem.recommendation
            holder.errorRecommendationTextView.visibility = View.VISIBLE
        } else {
            holder.errorRecommendationTextView.visibility = View.GONE
        }
    }




    override fun getItemCount(): Int = errorList.size
}

data class ErrorItem(
    val errorName: String,
    val errorCount: Int,
    val recommendation: String // Nueva propiedad para la recomendación

)
