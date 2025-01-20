package com.valdi.perfectrepapp.utils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.utils.adapters.Series // Cambia la importación a Series
import com.valdi.perfectrepapp.utils.adapters.SeriesInfoAdapter

class SeriesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val seriesList = mutableListOf<Series>()
    private lateinit var adapter: SeriesInfoAdapter
    private lateinit var sessionId: String
    private lateinit var exerciseTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSeries)
        recyclerView.layoutManager = LinearLayoutManager(this)

        sessionId = intent.getStringExtra("sessionId") ?: return
        exerciseTitle = intent.getStringExtra("exerciseTitle") ?: return

        adapter = SeriesInfoAdapter(seriesList) { serieToDelete ->
            showDeleteConfirmationDialog(serieToDelete)
        }
        recyclerView.adapter = adapter

        loadSeries()
    }

    private fun loadSeries() {
        db.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.email ?: "")
            .collection("workout_sessions")
            .document(sessionId)
            .collection("exercises")
            .whereEqualTo("exerciseTitle", exerciseTitle)
            .get()
            .addOnSuccessListener { exerciseDocuments ->
                for (exerciseDocument in exerciseDocuments) {
                    exerciseDocument.reference.collection("series")
                        .get()
                        .addOnSuccessListener { seriesDocuments ->
                            for (serieDocument in seriesDocuments) {
                                val serieData = serieDocument.toObject(Series::class.java)
                                seriesList.add(serieData)
                            }
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SeriesActivity", "Error obteniendo series: ", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SeriesActivity", "Error obteniendo el ejercicio: ", exception)
            }
    }

    private fun showDeleteConfirmationDialog(serie: Series) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar esta serie?")
            .setPositiveButton("Sí") { _, _ ->
                deleteSerie(serie)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSerie(serie: Series) {
        db.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.email ?: "")
            .collection("workout_sessions")
            .document(sessionId)
            .collection("exercises")
            .whereEqualTo("exerciseTitle", exerciseTitle)
            .get()
            .addOnSuccessListener { exerciseDocuments ->
                for (exerciseDocument in exerciseDocuments) {
                    exerciseDocument.reference.collection("series")
                        .whereEqualTo("seriesNumber", serie.seriesNumber)
                        .get()
                        .addOnSuccessListener { seriesDocuments ->
                            for (serieDocument in seriesDocuments) {
                                serieDocument.reference.delete()
                            }
                            seriesList.remove(serie)
                            adapter.notifyDataSetChanged()
                            Log.d("SeriesActivity", "Serie eliminada exitosamente: ${serie.seriesNumber}")
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SeriesActivity", "Error eliminando la serie: ", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SeriesActivity", "Error obteniendo el ejercicio para eliminar serie: ", exception)
            }
    }
}
