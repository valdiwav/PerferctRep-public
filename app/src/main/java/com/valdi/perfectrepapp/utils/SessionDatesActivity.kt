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
import com.valdi.perfectrepapp.utils.adapters.SessionDateAdapter

// En esta clase, obtendrás las sesiones de entrenamiento
import androidx.appcompat.app.AlertDialog

class SessionDatesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val sessionDates = mutableListOf<String>()
    private lateinit var adapter: SessionDateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_dates)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDates)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SessionDateAdapter(sessionDates, { selectedDate ->
            // Acción de clic para abrir actividad de ejercicios
            val intent = Intent(this, ExercisesActivity::class.java)
            intent.putExtra("sessionId", selectedDate)
            startActivity(intent)
        }, { dateToDelete ->
            showDeleteConfirmationDialog(dateToDelete)
        })
        recyclerView.adapter = adapter

        loadSessionDates()
    }




    private fun loadSessionDates() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection("users")
            .document(userEmail)
            .collection("workout_sessions")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val sessionId = document.id
                    checkAndAddSession(sessionId)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SessionDatesActivity", "Error obteniendo las sesiones: ", exception)
            }
    }

    private fun checkAndAddSession(sessionId: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Revisamos si la subcolección "exercises" contiene documentos
        db.collection("users")
            .document(userEmail)
            .collection("workout_sessions")
            .document(sessionId)
            .collection("exercises")
            .get()
            .addOnSuccessListener { exercises ->
                if (exercises.isEmpty) {
                    // Si la subcolección "exercises" está vacía, borramos la fecha
                    deleteSessionDate(sessionId)
                } else {
                    // Si contiene documentos, agregamos la fecha a la lista y actualizamos el adaptador
                    sessionDates.add(sessionId)
                    // Ordenamos la lista antes de actualizar el adaptador
                    sessionDates.sort()  // Ordena alfabéticamente. Cambia si usas otro formato de fecha
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SessionDatesActivity", "Error comprobando ejercicios de la sesión: ", exception)
            }
    }


    private fun showDeleteConfirmationDialog(date: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar todas las sesiones de esta fecha?")
            .setPositiveButton("Sí") { _, _ ->
                deleteSessionDate(date)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSessionDate(date: String) {
        db.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.email ?: "")
            .collection("workout_sessions")
            .document(date)
            .delete()
            .addOnSuccessListener {
                adapter.removeDate(date)
                Log.d("SessionDatesActivity", "Sesiones eliminada exitosamente: $date")
            }
            .addOnFailureListener { exception ->
                Log.e("SessionDatesActivity", "Error eliminando las sesiones de: ", exception)
            }
    }
}

