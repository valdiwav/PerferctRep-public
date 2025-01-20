package com.valdi.perfectrepapp.utils.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.valdi.perfectrepapp.R

class SeriesAdapter(private val seriesList: List<Serie>) :
    RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    private var expandedPosition = -1 // Variable para controlar qué ítem está expandido

    init {
        // Ordenar las series por número de serie
        seriesList.sortedBy { it.seriesNumber }
    }

    class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val seriesNumber: TextView = itemView.findViewById(R.id.seriesNumber)
        val reps: TextView = itemView.findViewById(R.id.reps)
        val weight: TextView = itemView.findViewById(R.id.weight)
        val rir: TextView = itemView.findViewById(R.id.rir)
        val detailsLayout: ConstraintLayout = itemView.findViewById(R.id.detailsLayout)
        val doneRIR: TextView = itemView.findViewById(R.id.didRIR)
        val doneReps: TextView = itemView.findViewById(R.id.repCountValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.serie_item, parent, false)
        return SeriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val serie = seriesList[position]
        holder.seriesNumber.text = "Serie ${serie.seriesNumber}"
        holder.reps.text = "${serie.reps}"
        holder.doneReps.text = "${serie.reps_done}"
        holder.weight.text = "${serie.weight}"
        holder.rir.text = "${serie.RIR}"
        if (serie.RIR_done!="4"){
            holder.doneRIR.text = "${serie.RIR_done}"
        }else{
            holder.doneRIR.text = "> 3 Lejos del fallo"
        }




        // Verificar si la posición actual está expandida
        val isExpanded = position == expandedPosition
        holder.detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Control de expansión y colapso
        holder.seriesNumber.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyDataSetChanged() // Actualizar el RecyclerView para reflejar los cambios
        }
    }

    override fun getItemCount(): Int {
        return seriesList.size
    }
}


data class Serie(
    val RIR: String = "",
    val reps: String = "",
    val seriesNumber: Int = 0,
    val timestamp: String = "",
    val weight: String = "",
    val RIR_done: String = "",
    val reps_done: String = ""
)