package com.valdi.perfectrepapp.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.R
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class ExerciseInfoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_exercise_info)

        // Inicializar Firestore y FirebaseAuth
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Encontrar las vistas
        val exerciseTitleTextView = findViewById<TextView>(R.id.exerciseTitleTextView)
        val exerciseDescriptionTextView = findViewById<TextView>(R.id.exerciseDescriptionTextView)
        val startSeriesButton = findViewById<Button>(R.id.startSeriesButton)

        // Inputs
        val workoutWeight = findViewById<TextInputEditText>(R.id.workout_weight_edit_text)
        val workoutRIR = findViewById<TextInputEditText>(R.id.workout_RIR_edit_text)
        val workoutReps = findViewById<TextInputEditText>(R.id.reps_edit_text)

        // Obtener los datos del intent
        val title = intent.getStringExtra("title") ?: "Título no disponible"
        val description = intent.getStringExtra("description") ?: "Descripción no disponible"
        val videoUrl = intent.getStringExtra("videoUrl")
        val videoIframe = videoUrl ?: "<h1>Video no disponible</h1>"

        // Establecer los datos en la interfaz
        exerciseTitleTextView.text = title
        exerciseDescriptionTextView.text = description

        val webView: WebView = findViewById(R.id.exerciseWebView)
        webView.loadData(videoIframe, "text/html", "utf-8")
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()

        // Redirigir a PoseDetectionActivity al hacer clic en el botón
        startSeriesButton.setOnClickListener {
            // Capturar datos de los inputs
            val weight = workoutWeight.text.toString()
            val rir = workoutRIR.text.toString()
            val reps = workoutReps.text.toString()

            // Validaciones
            if (weight.isEmpty() || rir.isEmpty() || reps.isEmpty()) {
                Toast.makeText(this, "Todos los campos deben estar completos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weightValue = weight.toIntOrNull() ?: 0
            val rirValue = rir.toIntOrNull() ?: 0
            val repsValue = reps.toIntOrNull() ?: 0

            // Validar límites lógicos
            if (weightValue < 1 || weightValue > 500) {
                Toast.makeText(this, "El peso debe estar entre 1 y 500 kg", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rirValue < 0 || rirValue > 10) {
                Toast.makeText(this, "El RIR debe estar entre 0 y 10", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (repsValue < 1 || repsValue > 30) {
                Toast.makeText(this, "Las repeticiones deben estar entre 1 y 30", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Asegurarte de que el usuario esté autenticado
            val user = auth.currentUser
            if (user != null) {
                val email = user.email ?: return@setOnClickListener

                // Generar un ID de sesión (puedes usar la fecha y hora actuales)
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sessionId = dateFormatter.format(Date())

                // El nombre del ejercicio que se está realizando (e.g., "curl", "press", etc.)
                val exerciseTitle = title

                // Obtener la colección de series de este ejercicio específico
                db.collection("users")
                    .document(email)
                    .collection("workout_sessions")
                    .document(sessionId)
                    .collection("exercises")
                    .document(exerciseTitle)
                    .collection("series")
                    .get()
                    .addOnSuccessListener { seriesDocuments ->
                        // Determinar el siguiente número de serie basado en la cantidad de documentos en la subcolección de series
                        val nextSeriesNumber = seriesDocuments.size() + 1
                        val seriesId = "serie$nextSeriesNumber"

                        // Crear un mapa con los datos del ejercicio
                        val workoutData = hashMapOf(
                            "weight" to weight,
                            "RIR" to rir,
                            "reps" to reps,
                            "seriesNumber" to nextSeriesNumber,
                            "timestamp" to System.currentTimeMillis()
                                .toString() // Guardar el timestamp
                        )

                        // Crear un objeto con el título del ejercicio
                        val exerciseName = hashMapOf(
                            "exerciseTitle" to exerciseTitle
                        )

                        // Referencia al documento de workout_sessions
                        val workoutSessionRef = db.collection("users")
                            .document(email)
                            .collection("workout_sessions")
                            .document(sessionId)

                        // Primero, guardar el sessionId en el documento de workout_sessions
                        workoutSessionRef.set(
                            hashMapOf("sessionId" to sessionId),
                            SetOptions.merge()
                        )
                            .addOnSuccessListener {
                                Log.d(
                                    "Firestore",
                                    "sessionId guardado correctamente en workout_sessions"
                                )

                                // Guardar los datos en Firestore bajo la subcolección de ejercicios del usuario
                                val exercisesRef = workoutSessionRef.collection("exercises")

                                // Ahora, guardar el nombre del ejercicio en el documento principal
                                exercisesRef.document(exerciseTitle) // El nombre del ejercicio (e.g., "curl", "press", etc.)
                                    .set(exerciseName) // Esto guardará el campo exerciseTitle
                                    .addOnSuccessListener {
                                        // Ahora, guardar los datos en la subcolección "series"
                                        exercisesRef.document(exerciseTitle)
                                            .collection("series") // Subcolección "series" para cada ejercicio
                                            .document(seriesId) // Usa "serieX" como ID del documento
                                            .set(workoutData)
                                            .addOnSuccessListener {
                                                // Los datos de la serie se guardaron correctamente
                                                Log.d(
                                                    "Firestore",
                                                    "Datos del ejercicio guardados correctamente"
                                                )

                                                // Redirigir a PoseDetectionActivity
                                                val intent = Intent(
                                                    this,
                                                    CountdownActivity::class.java
                                                ).apply {
                                                    putExtra("title", exerciseTitle)
                                                }
                                                startActivity(intent)
                                            }
                                            .addOnFailureListener { e ->
                                                // Hubo un error al guardar los datos de la serie
                                                Log.e(
                                                    "Firestore",
                                                    "Error al guardar los datos de la serie",
                                                    e
                                                )
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        // Hubo un error al guardar el nombre del ejercicio
                                        Log.e(
                                            "Firestore",
                                            "Error al guardar el nombre del ejercicio",
                                            e
                                        )
                                    }
                            }
                            .addOnFailureListener { e ->
                                // Hubo un error al guardar sessionId en workout_sessions
                                Log.e(
                                    "Firestore",
                                    "Error al guardar sessionId en workout_sessions",
                                    e
                                )
                            }

                    }
            }
        }

    }


}

