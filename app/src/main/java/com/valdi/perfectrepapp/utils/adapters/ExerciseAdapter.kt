package com.valdi.perfectrepapp.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.valdi.perfectrepapp.R

class ExerciseAdapter(
    private val exercises: List<String>,
    private val onExerciseClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_info_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise)
    }

    override fun getItemCount() = exercises.size

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exerciseTitleTextView: TextView = itemView.findViewById(R.id.exerciseTitleTextView)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton) // Añade un botón en el layout

        fun bind(exercise: String) {
            exerciseTitleTextView.text = exercise
            itemView.setOnClickListener {
                onExerciseClick(exercise)
            }
            deleteButton.setOnClickListener {
                onDeleteClick(exercise)
            }
        }
    }
}

