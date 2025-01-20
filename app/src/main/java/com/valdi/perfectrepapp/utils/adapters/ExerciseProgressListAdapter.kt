package com.valdi.perfectrepapp.utils.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.ui.screens.progressScreens.ProgressActivity

class ExerciseProgressListAdapter(private var exercises: List<String>) :
    RecyclerView.Adapter<ExerciseProgressListAdapter.ExerciseViewHolder>() {

    // Orden deseado de los ejercicios
    private val exerciseOrder = listOf("Sentadillas", "Curl de Bíceps", "Peso Muerto", "Press de Banca")

    // Actualiza y ordena la lista de ejercicios
    fun updateExercises(newExercises: List<String>) {
        exercises = newExercises.sortedBy { exerciseOrder.indexOf(it).takeIf { it >= 0 } ?: Int.MAX_VALUE }
        notifyDataSetChanged()
    }

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseImage: ImageView = itemView.findViewById(R.id.imageView)
        val exerciseTitle: TextView = itemView.findViewById(R.id.titleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val title = exercises[position] // Usar la lista ordenada
        holder.exerciseTitle.text = title

        // Cambia la imagen según el título del ejercicio
        val imageResId = when (title) {
            "Sentadillas" -> R.drawable.squats
            "Curl de Bíceps" -> R.drawable.bicep_curl
            "Peso Muerto" -> R.drawable.dead_lift
            "Press de Hombro" -> R.drawable.showlder_press
            else -> R.drawable.tfl2_logo // Imagen predeterminada si no coincide ningún título
        }
        holder.exerciseImage.setImageResource(imageResId)

        // Maneja el clic para lanzar la actividad correspondiente
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = when (title) {
                "Sentadillas" -> Intent(context, ProgressActivity::class.java).apply {
                    putExtra("exerciseTitle",title)
                }
                "Curl de Bíceps" -> Intent(context, ProgressActivity::class.java).apply {
                    putExtra("exerciseTitle",title)
                }
                "Peso Muerto" -> Intent(context, ProgressActivity::class.java).apply {
                    putExtra("exerciseTitle",title)
                }
                "Press de Hombro" -> Intent(context, ProgressActivity::class.java).apply {
                    putExtra("exerciseTitle",title)
                }
                else -> null
            }
            intent?.let { context.startActivity(it) }
        }
    }

    override fun getItemCount(): Int = exercises.size
}
