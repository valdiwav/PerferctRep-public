package com.valdi.perfectrepapp.utils

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.utils.adapters.ExerciseAdapter

// Esta clase obtiene los ejercicios de una sesión específica (fecha seleccionada)
import androidx.appcompat.app.AlertDialog

class ExercisesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val exercises = mutableListOf<String>()
    private lateinit var adapter: ExerciseAdapter
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercises)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewExercises)
        recyclerView.layoutManager = LinearLayoutManager(this)

        sessionId = intent.getStringExtra("sessionId") ?: return
        adapter = ExerciseAdapter(exercises, { selectedExercise ->
            // Acción de clic para ver series del ejercicio
            val intent = Intent(this, SeriesActivity::class.java)
            intent.putExtra("sessionId", sessionId)
            intent.putExtra("exerciseTitle", selectedExercise)
            startActivity(intent)
        }, { exerciseToDelete ->
            // Acción de clic para eliminar un ejercicio
            showDeleteConfirmationDialog(exerciseToDelete)
        })
        recyclerView.adapter = adapter

        loadExercises()
    }

    private fun loadExercises() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection("users")
            .document(userEmail)
            .collection("workout_sessions").document(sessionId)
            .collection("exercises")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val exerciseId = document.id
                    val exerciseTitle = document.getString("exerciseTitle") ?: "Ejercicio sin título"
                    checkAndAddExercise(exerciseId, exerciseTitle)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ExercisesActivity", "Error obteniendo ejercicios: ", exception)
            }
    }

    private fun checkAndAddExercise(exerciseId: String, exerciseTitle: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Revisamos si la subcolección "series" contiene documentos
        db.collection("users")
            .document(userEmail)
            .collection("workout_sessions").document(sessionId)
            .collection("exercises").document(exerciseId)
            .collection("series")
            .get()
            .addOnSuccessListener { series ->
                if (series.isEmpty) {
                    // Si la subcolección "series" está vacía, borramos el ejercicio
                    deleteExercise(exerciseTitle)
                } else {
                    // Si contiene documentos, agregamos el ejercicio a la lista y actualizamos el adaptador
                    exercises.add(exerciseTitle)
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ExercisesActivity", "Error comprobando series del ejercicio: ", exception)
            }
    }

    private fun showDeleteConfirmationDialog(exercise: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este ejercicio?")
            .setPositiveButton("Sí") { _, _ ->
                deleteExercise(exercise)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteExercise(exercise: String) {
        db.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.email ?: "")
            .collection("workout_sessions").document(sessionId)
            .collection("exercises")
            .whereEqualTo("exerciseTitle", exercise)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                exercises.remove(exercise)
                adapter.notifyDataSetChanged()
                Log.d("ExercisesActivity", "Ejercicio eliminado exitosamente: $exercise")
            }
            .addOnFailureListener { exception ->
                Log.e("ExercisesActivity", "Error eliminando el ejercicio: ", exception)
            }
    }
}

