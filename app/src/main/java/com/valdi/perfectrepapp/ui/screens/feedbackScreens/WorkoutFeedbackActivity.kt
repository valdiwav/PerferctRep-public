package com.valdi.perfectrepapp.ui.screens.feedbackScreens

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.navigation.BottomNavActivity
import com.valdi.perfectrepapp.utils.adapters.Serie
import com.valdi.perfectrepapp.utils.adapters.SeriesAdapter
import java.text.SimpleDateFormat
import java.util.*

class WorkoutFeedbackActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var seriesAdapter: SeriesAdapter
    private val seriesList = mutableListOf<Serie>()
    private lateinit var auth: FirebaseAuth

    private lateinit var totalRepsTextView: TextView
    private lateinit var averageWeightTextView: TextView
    private lateinit var averageRirTextView: TextView
    private lateinit var exerciseTitle: TextView
    private lateinit var backHomeBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_feedback)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializar los TextViews para el resumen
        totalRepsTextView = findViewById(R.id.totalReps)
        averageWeightTextView = findViewById(R.id.averageWeight)
        averageRirTextView = findViewById(R.id.averageRir)
        exerciseTitle = findViewById(R.id.exerciseTitle)
        backHomeBtn = findViewById(R.id.backHome)

        // Inicializar el RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backHomeBtn.setOnClickListener {
            showExitConfirmationDialog()
        }

        // Obtener el correo del usuario autenticado
        val currentUser = auth.currentUser
        val userEmail = currentUser?.email

        if (userEmail == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener la fecha actual en formato yy-mm-dd
        val sessionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Obtener el título del ejercicio pasado desde el intent
        val exerciseName = intent.getStringExtra("title")
        exerciseTitle.text = exerciseName ?:"Título no encontrado"

        if (exerciseName == null) {
            Toast.makeText(this, "No se recibió el nombre del ejercicio", Toast.LENGTH_SHORT).show()
            return
        }

        // Ruta a la subcolección de series
        val seriesRef = firestore.collection("users")
            .document(userEmail)
            .collection("workout_sessions")
            .document(sessionDate)
            .collection("exercises")
            .document(exerciseName)
            .collection("series")

        // Obtener las series desde Firestore
        seriesRef.get()
            .addOnSuccessListener { documents ->
                var totalReps = 0
                var totalWeight = 0.0
                var totalRir = 0.0
                var seriesCount = 0

                for (document in documents) {
                    val serie = document.toObject(Serie::class.java)
                    seriesList.add(serie)

                    // Sumar repeticiones y convertir el valor de reps a entero
                    totalReps += serie.reps_done?.toIntOrNull() ?: 0

                    // Sumar el peso total
                    totalWeight += serie.weight?.toDoubleOrNull() ?: 0.0

                    // Sumar el RIR total
                    totalRir += serie.RIR_done?.toDoubleOrNull() ?: 0.0

                    // Contar el número de series
                    seriesCount++
                }

                // Ordenar las series numéricamente por el campo seriesNumber
                seriesList.sortBy { it.seriesNumber }

                // Configurar el adaptador con la lista ordenada
                seriesAdapter = SeriesAdapter(seriesList)
                recyclerView.adapter = seriesAdapter

                // Calcular el promedio de peso y RIR
                val averageWeight = if (seriesCount > 0) totalWeight / seriesCount else 0.0
                val averageRir = if (seriesCount > 0) totalRir / seriesCount else 0.0

                // Mostrar los resultados en los TextViews
                totalRepsTextView.text = "Total de Repeticiones: $totalReps"
                averageWeightTextView.text = "Peso Promedio: %.2f".format(averageWeight)
                averageRirTextView.text = "RIR Promedio: %.2f".format(averageRir)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener las series: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Finalizar Entrenamiento")
        builder.setMessage("¿Estás seguro de que quieres volver al inicio?")

        // Configurar el botón "Sí"
        builder.setPositiveButton("Sí") { dialog: DialogInterface, _: Int ->
            // Navegar a BottomNavActivity
            val intent = Intent(this, BottomNavActivity::class.java)
            startActivity(intent)
            finish() // Opcional: cierra la actividad actual si no quieres volver a ella
        }

        // Configurar el botón "No"
        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int ->
            dialog.dismiss() // Cierra el diálogo
        }

        // Mostrar el diálogo
        val dialog = builder.create()
        dialog.show()
    }
}
