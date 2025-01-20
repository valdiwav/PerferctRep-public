package com.valdi.perfectrepapp.ui.screens.feedbackScreens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.ui.screens.CountdownActivity
import com.valdi.perfectrepapp.ui.screens.PoseDetectionActivity
import com.valdi.perfectrepapp.utils.adapters.ErrorAdapter
import com.valdi.perfectrepapp.utils.adapters.ErrorItem
import java.text.SimpleDateFormat
import java.util.*

class SetFeedbackActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private var setCount = 1 // Contador de sets
    private lateinit var errorRecyclerView: RecyclerView
    private lateinit var errorAdapter: ErrorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_feedback)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar RecyclerView
        errorRecyclerView = findViewById(R.id.errorRecyclerView)
        errorRecyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        val endSetBtn = findViewById<Button>(R.id.endWorkoutBtn)
        val startNextSetBtn = findViewById<Button>(R.id.startNextSetBtn)
        val exerciseTitleTextView = findViewById<TextView>(R.id.exerciseTitleTextView)
        val repCountValueTextView = findViewById<TextView>(R.id.repCountValue)
        val objectiveReps = findViewById<TextView>(R.id.objectiveReps)
        val objectiveRIR = findViewById<TextView>(R.id.objectiveRIR)
        val workout_weightTextView = findViewById<TextView>(R.id.workout_weight)
        val currentSet = findViewById<TextView>(R.id.currentSet)
        val workout_weightEditText = findViewById<TextView>(R.id.workout_weight_edit_text)
        val repsEditText = findViewById<TextView>(R.id.repsEditText)
        val rirEditText = findViewById<TextView>(R.id.rirEditText)
        val doneRIR = findViewById<TextView>(R.id.didRIR)
        val perfectRep = findViewById<TextView>(R.id.perfectRep)
        val totalErrors = findViewById<TextView>(R.id.errorsCount)





        // Obtener el título del ejercicio de la intención
        val workoutTitle = intent.getStringExtra("title")
        exerciseTitleTextView.text = workoutTitle ?: "Título no encontrado"

        // Obtener repCountValue de la intención
        val repCountValue = intent.getStringExtra("repCount") ?: "0"
        repCountValueTextView.text = repCountValue
        Log.d("SetFeedbackActivity", "Valor de repCount: $repCountValue")

        val lastRIRValue = intent.getStringExtra("lastRIR") ?: "0"
        if (lastRIRValue!="4"){
            doneRIR.text = lastRIRValue
        }else{
            doneRIR.text = "> 3 Lejos del fallo"
        }
        Log.d("SetFeedbackActivity", "lastRIRValue: $lastRIRValue")

        val perfectRepValue = intent.getStringExtra("perfectRep") ?: "0"
        perfectRep.text = perfectRepValue

        val totalErrorsValue = intent.getStringExtra("errorRepCount") ?: "0"
        totalErrors.text = totalErrorsValue


        // Obtener los valores de error (simulando los valores recibidos)
        val elbowErrorCount = intent.getStringExtra("errorCount")?.toIntOrNull() ?: 0
        val speedErrorCount = intent.getStringExtra("speedError")?.toIntOrNull() ?: 0
        val balanceErrorCount = intent.getStringExtra("balanceError")?.toIntOrNull() ?: 0


        // Obtener los valores de error (simulando los valores recibidos)
        val kneeErrorCount = intent.getStringExtra("KneeErrorCountValue")?.toIntOrNull() ?: 0
        val legSpeedError = intent.getStringExtra("legSpeedErrorCountValue")?.toIntOrNull() ?: 0
        val forceImbalanceErrorCount = intent.getStringExtra("forceImbalanceErrorCountValue")?.toIntOrNull() ?: 0

        val shoulderSpeedErrorValue = intent.getStringExtra("shoulderSpeedErrorValue")?.toIntOrNull() ?: 0
        val shoulderElbowErrorValue = intent.getStringExtra("shoulderElbowErrorValue")?.toIntOrNull() ?: 0
        val shoulderBalanceErrorValue = intent.getStringExtra("shoulderBalanceErrorValue")?.toIntOrNull() ?: 0


        val deadLiftSpeedErrorValue = intent.getStringExtra("deadLiftSpeedErrorValue")?.toIntOrNull() ?: 0
        val barDistanceErrorCount = intent.getStringExtra("barDistanceErrorCount")?.toIntOrNull() ?: 0
        val backErrorCount = intent.getStringExtra("backErrorCount")?.toIntOrNull() ?: 0

        // Crear lista vacía para errores
        val errorList = mutableListOf<ErrorItem>()

        // Consultar recomendaciones en Firestore usando el título del ejercicio
        db.collection("recommendations")
            .document(workoutTitle ?: return)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Verificar cada error y añadir recomendaciones si el conteo es mayor a 0
                    if (elbowErrorCount > 0) {
                        val elbowRecommendation = document.getString("elbowError") ?: "Error en codos"
                        errorList.add(ErrorItem("Codos elevados", elbowErrorCount, elbowRecommendation))
                    }
                    if (speedErrorCount > 0) {
                        val speedRecommendation = document.getString("speedError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Ejecuciones rápidas", speedErrorCount, speedRecommendation))
                    }
                    if (balanceErrorCount > 0) {
                        val balanceRecommendation = document.getString("balanceError") ?: "Error de balance"
                        errorList.add(ErrorItem("Balanceo del cuerpo", balanceErrorCount, balanceRecommendation))
                    }
                    if (kneeErrorCount > 0) {
                        val kneeRecommendation = document.getString("kneeError") ?: "Error de rodilla"
                        errorList.add(ErrorItem("Rodillas juntas", kneeErrorCount, kneeRecommendation))
                    }
                    if (legSpeedError > 0) {
                        val legSpeedRecommendation = document.getString("legSpeedError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Ejecuciones rápidas", legSpeedError, legSpeedRecommendation))
                    }
                    if (forceImbalanceErrorCount > 0) {
                        val forceImbalanceRecommendation = document.getString("forceImbalanceError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Desequilibrio de fuerzas", forceImbalanceErrorCount, forceImbalanceRecommendation))
                    }
                    if (shoulderSpeedErrorValue > 0) {
                        val shoulderSpeedErrorRecommendation = document.getString("shoulderSpeedError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Ejecuciones rápidas", shoulderSpeedErrorValue, shoulderSpeedErrorRecommendation))
                    }
                    if (shoulderElbowErrorValue > 0) {
                        val shoulderElbowErrorRecommendation = document.getString("shoulderElbowError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Codos separados del torso", shoulderElbowErrorValue, shoulderElbowErrorRecommendation))
                    }
                    if (shoulderBalanceErrorValue > 0) {
                        val shoulderBalanceErrorRecommendation = document.getString("shoulderBalanceError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Balanceo del cuerpo", shoulderBalanceErrorValue, shoulderBalanceErrorRecommendation))
                    }
                    if (deadLiftSpeedErrorValue > 0) {
                        val deadLiftSpeedErrorRecommendation = document.getString("deadLiftSpeedError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Ejecuciones rápidas", deadLiftSpeedErrorValue, deadLiftSpeedErrorRecommendation))
                    }
                    if (barDistanceErrorCount > 0) {
                        val barDistanceErrorRecommendation = document.getString("barDistanceError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Barra alejada del cuerpo", barDistanceErrorCount, barDistanceErrorRecommendation))
                    }
                    if (backErrorCount > 0) {
                        val backErrorRecommendation = document.getString("backError") ?: "Error en velocidad"
                        errorList.add(ErrorItem("Espalda encorvada", backErrorCount, backErrorRecommendation))
                    }

                } else {
                    Log.d("Firestore", "No se encontraron recomendaciones para el ejercicio.")
                }

                // Si no hay errores, mostrar mensaje sin recomendaciones
                if (errorList.isEmpty()) {
                    errorList.add(ErrorItem("No se detectaron errores", 0, ""))
                }

                // Configurar el adaptador del RecyclerView con la lista de errores
                errorAdapter = ErrorAdapter(errorList)
                errorRecyclerView.adapter = errorAdapter
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener recomendaciones: ", e)
            }

        // Configurar el adaptador del RecyclerView con la lista de errores
        errorAdapter = ErrorAdapter(errorList)
        errorRecyclerView.adapter = errorAdapter


        // Obtener la fecha actual
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Asegurarte de que el usuario esté autenticado
        val user = auth.currentUser
        if (user != null) {
            val email = user.email ?: return

            // Recuperar el último ejercicio usando el timestamp
            db.collection("users")
                .document(email)
                .collection("workout_sessions")
                .document(currentDate)
                .collection("exercises")
                .document(workoutTitle ?: return)
                .collection("series")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1) // Obtener solo la última serie
                .get()
                .addOnSuccessListener { seriesDocuments ->
                    if (seriesDocuments.isEmpty) {
                        Log.d("SetFeedbackActivity", "No hay series registradas para este ejercicio.")
                    } else {
                        for (document in seriesDocuments) {

                            // Obtener referencia del documento de la última serie
                            val lastSeriesRef = document.reference

                            // Obtener los datos de la última serie
                            objectiveReps.text = document.getString("reps") ?: "0"
                            objectiveRIR.text = document.getString("RIR") ?: "0"
                            workout_weightTextView.text = document.getString("weight") ?: "0"
                            val seriesNumber = document.getLong("seriesNumber")?.toInt()?.toString() ?: "0"
                            currentSet.text = seriesNumber

                            // Parte donde defines `newFieldData` para incluir los errores
                            val newFieldData = mutableMapOf<String, Any>(
                                "reps_done" to repCountValue,
                                "RIR_done" to lastRIRValue
                            )

                            // Crear un mapa para almacenar solo los errores significativos
                            val errors = mutableMapOf<String, Int>()

                            // Agregar errores específicos al mapa de errores si el conteo es mayor a 1
                            if (elbowErrorCount > 0) {
                                errors["elbowError"] = elbowErrorCount
                            }

                            if (speedErrorCount > 0) {
                                errors["speedError"] = speedErrorCount
                            }

                            if (balanceErrorCount > 0) {
                                errors["balanceError"] = balanceErrorCount
                            }

                            if (balanceErrorCount > 0) {
                                errors["balanceError"] = balanceErrorCount
                            }
                            if (kneeErrorCount > 0) {
                                errors["KneeErrorCountValue"] = kneeErrorCount
                            }
                            if (legSpeedError > 0) {
                                errors["legSpeedErrorCountValue"] = legSpeedError
                            }
                            if (forceImbalanceErrorCount > 0) {
                                errors["forceImbalanceErrorCountValue"] = forceImbalanceErrorCount
                            }

                            if (shoulderSpeedErrorValue > 0) {
                                errors["shoulderSpeedErrorValue"] = shoulderSpeedErrorValue
                            }
                            if (shoulderElbowErrorValue > 0) {
                                errors["shoulderElbowErrorValue"] = shoulderElbowErrorValue
                            }
                            if (shoulderBalanceErrorValue > 0) {
                                errors["shoulderBalanceErrorValue"] = shoulderBalanceErrorValue
                            }


                            if (deadLiftSpeedErrorValue > 0) {
                                errors["deadLiftSpeedErrorValue"] = deadLiftSpeedErrorValue
                            }
                            if (barDistanceErrorCount > 0) {
                                errors["barDistanceErrorCount"] = barDistanceErrorCount
                            }
                            if (backErrorCount > 0) {
                                errors["backErrorCount"] = backErrorCount
                            }

                            // Solo agregar el mapa de errores si contiene algún error
                            if (errors.isNotEmpty()) {
                                newFieldData["errors"] = errors
                            }


                            // Guardar o actualizar el dato en el mismo documento
                            lastSeriesRef.update(newFieldData)
                                .addOnSuccessListener {
                                    Log.d("SetFeedbackActivity", "Dato guardado exitosamente en la última serie.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("SetFeedbackActivity", "Error al guardar el dato en la última serie", e)
                                }

                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SetFeedbackActivity", "Error al recuperar los datos de la serie", e)
                }
        }

        endSetBtn.setOnClickListener {
            // Mostrar el diálogo de confirmación
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Finalizar ejercicio")
            builder.setMessage("¿Estás seguro de que quieres finalizar el ejercicio?")

            // Opción "Sí"
            builder.setPositiveButton("Sí") { dialog, which ->
                // Desactivar el botón para evitar múltiples clics
                startNextSetBtn.isEnabled = false

                // Guardar el contador en SharedPreferences
                val sharedPreferences = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putInt("setCount", setCount)
                    apply()
                }

                val workoutIntent = Intent(this, WorkoutFeedbackActivity::class.java).apply {
                    putExtra("title", workoutTitle)
                }
                startActivity(workoutIntent)
                finish()
            }

            // Opción "No"
            builder.setNegativeButton("No") { dialog, which ->
                // Si el usuario presiona "No", solo cerramos el diálogo y no hacemos nada más
                dialog.dismiss()
            }

            // Mostrar el diálogo
            builder.show()
        }


        startNextSetBtn.setOnClickListener {
            // Obtener los datos de los inputs
            val weight = workout_weightEditText.text.toString()
            val reps = repsEditText.text.toString()
            val rir = rirEditText.text.toString()

            // Validaciones
            if (weight.isEmpty() || reps.isEmpty() || rir.isEmpty()) {
                Toast.makeText(this, "Todos los campos deben estar completos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weightValue = weight.toIntOrNull() ?: 0
            val repsValue = reps.toIntOrNull() ?: 0
            val rirValue = rir.toIntOrNull() ?: 0

            // Validar límites lógicos
            if (weightValue < 1 || weightValue > 500) {
                Toast.makeText(this, "El peso debe estar entre 1 y 500 kg/lb", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (repsValue < 1 || repsValue > 30) {
                Toast.makeText(this, "Las repeticiones deben estar entre 1 y 30", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rirValue < 0 || rirValue > 10) {
                Toast.makeText(this, "El RIR debe estar entre 0 y 10", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Asegurarte de que el usuario esté autenticado
            val user = auth.currentUser
            if (user != null) {
                val email = user.email ?: return@setOnClickListener

                // Obtener la fecha actual
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Guardar el contador en SharedPreferences
                val sharedPreferences = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putInt("setCount", setCount)
                    apply()
                }

                // Recuperar o crear el ejercicio y guardar la serie
                val exerciseTitle = workoutTitle ?: return@setOnClickListener

                // Obtener la colección de series de este ejercicio específico
                db.collection("users")
                    .document(email)
                    .collection("workout_sessions")
                    .document(currentDate)
                    .collection("exercises")
                    .document(exerciseTitle)
                    .collection("series")
                    .get()
                    .addOnSuccessListener { seriesDocuments ->
                        // Determinar el siguiente número de serie basado en la cantidad de documentos en la subcolección de series
                        val nextSeriesNumber = seriesDocuments.size() + 1
                        val seriesId = "serie$nextSeriesNumber"

                        // Crear un mapa con los datos de la nueva serie
                        val seriesData = hashMapOf(
                            "weight" to weight,
                            "reps" to reps,
                            "RIR" to rir,
                            "seriesNumber" to nextSeriesNumber,
                            "timestamp" to System.currentTimeMillis().toString() // Guardar el timestamp
                        )

                        // Guardar los datos de la serie en Firestore
                        db.collection("users")
                            .document(email)
                            .collection("workout_sessions")
                            .document(currentDate)
                            .collection("exercises")
                            .document(exerciseTitle)
                            .collection("series")
                            .document(seriesId)
                            .set(seriesData)
                            .addOnSuccessListener {
                                // Los datos se guardaron correctamente
                                Log.d("Firestore", "Datos de la serie guardados correctamente")

                                // Actualizar los inputs para la siguiente serie
                                repsEditText.text = null
                                rirEditText.text = null
                                workout_weightEditText.text = null

                                Toast.makeText(this, "Serie guardada correctamente.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                // Hubo un error al guardar los datos
                                Log.e("Firestore", "Error al guardar los datos", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        // Hubo un error al obtener la cantidad de series
                        Log.e("Firestore", "Error al obtener la cantidad de series", e)
                    }

                val poseDetectionIntent = Intent(this, CountdownActivity::class.java).apply {
                    putExtra("title", workoutTitle)
                }
                startActivity(poseDetectionIntent)
                finish()
            }
        }
    }




        override fun onResume() {
        super.onResume()
        // Rehabilitar el botón al volver a esta actividad
        findViewById<Button>(R.id.startNextSetBtn).isEnabled = true
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        Toast.makeText(this, "Presiona FINALIZAR EJERCICIO para salir ", Toast.LENGTH_SHORT).show()
    }


}

