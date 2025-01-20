package com.valdi.perfectrepapp.utils.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.ui.screens.ExerciseInfoActivity

class ExerciseListAdapter(
    private val imageUrls: List<String>,
    private val titles: List<String>,
    private val descriptions: List<String>,
    private val videoUrls: List<String>, // Añadir lista de videoUrls
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<ExerciseListAdapter.ExerciseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_image, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        val title = titles[position]
        val description = descriptions[position]
        val videoUrl = videoUrls[position] // Obtener el videoUrl correspondiente

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .priority(Priority.IMMEDIATE)
            .into(holder.imageView)

        holder.titleTextView.text = title

        holder.imageView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ExerciseInfoActivity::class.java).apply {
                putExtra("title", title)
                putExtra("description", description)
                putExtra("videoUrl", videoUrl) // Pasar el videoUrl al intent
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = imageUrls.size

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
    }
}

