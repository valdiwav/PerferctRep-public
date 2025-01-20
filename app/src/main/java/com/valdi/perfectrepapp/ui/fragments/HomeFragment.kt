package com.valdi.perfectrepapp.ui.fragments

import com.valdi.perfectrepapp.utils.adapters.ExerciseProgressListAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.valdi.perfectrepapp.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var exerciseListAdapter: ExerciseProgressListAdapter
    private val exerciseTitles = mutableListOf<String>()
    private lateinit var userNameTextView: TextView
    private lateinit var noExercisesTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewExercises)
        recyclerView.layoutManager = LinearLayoutManager(context)
        userNameTextView = view.findViewById(R.id.textViewUserName)
        noExercisesTextView = view.findViewById(R.id.textViewNoExercises)


        // Initialize adapter
        exerciseListAdapter = ExerciseProgressListAdapter(exerciseTitles)
        recyclerView.adapter = exerciseListAdapter

        loadUserName() // Cargar el nombre del usuario

        loadExercises() // Cargar los ultimos 7 ejercicios

        return view
    }

    private fun loadExercises() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        val email = user?.email ?: return

        Log.d("HomeFragment", "Usuario: $email")

        // Ordenar por el ID del documento (que debería ser una fecha) en orden descendente para obtener los más recientes y limitar a 7
        db.collection("users").document(email)
            .collection("workout_sessions")
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING) // Orden descendente
            .limit(7) // Limitar a los últimos 7 documentos
            .get()
            .addOnSuccessListener { dateDocuments ->
                Log.d("HomeFragment", "Se obtuvieron las fechas correctamente. Total de fechas: ${dateDocuments.size()}")
                exerciseTitles.clear() // Limpiar lista existente

                // Verificar si no hay documentos y mostrar el mensaje
                if (dateDocuments.isEmpty) {
                    noExercisesTextView.visibility = View.VISIBLE // Mostrar mensaje de "sin ejercicios"
                    recyclerView.visibility = View.GONE // Ocultar RecyclerView
                    exerciseListAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                } else {
                    noExercisesTextView.visibility = View.GONE // Ocultar mensaje
                    recyclerView.visibility = View.VISIBLE // Mostrar RecyclerView
                }

                // Utilizar un Set para evitar duplicados
                val uniqueExerciseTitles = mutableSetOf<String>()

                // Recorrer cada uno de los 7 documentos de fecha
                for (dateDocument in dateDocuments) {
                    val date = dateDocument.id // El id del documento es la fecha
                    Log.d("HomeFragment", "Procesando fecha: $date")

                    // Obtener ejercicios de la fecha actual
                    db.collection("users").document(email)
                        .collection("workout_sessions").document(date)
                        .collection("exercises")
                        .get()
                        .addOnSuccessListener { exerciseDocuments ->
                            Log.d("HomeFragment", "Se obtuvieron los ejercicios de la fecha $date. Total de ejercicios: ${exerciseDocuments.size()}")

                            if (exerciseDocuments.isEmpty) {
                                Log.d("HomeFragment", "No se encontraron ejercicios en la fecha $date.")
                            }

                            for (exerciseDocument in exerciseDocuments) {
                                exerciseDocument.getString("exerciseTitle")?.let { title ->
                                    uniqueExerciseTitles.add(title) // Agregar a Set
                                    Log.d("HomeFragment", "Ejercicio añadido: $title")
                                }
                            }

                            // Actualizar la lista con los títulos únicos
                            exerciseTitles.clear()
                            exerciseTitles.addAll(uniqueExerciseTitles)

                            exerciseListAdapter.notifyDataSetChanged() // Notificar cambios
                        }
                        .addOnFailureListener { exception ->
                            Log.e("HomeFragment", "Error al obtener ejercicios en la fecha $date: ", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error al obtener sesiones de entrenamiento: ", exception)
            }
    }

    private fun getUserName(email: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("name") ?: ""
                    // Guardar solo el primer nombre (hasta el primer espacio)
                    val firstName = fullName.split(" ").firstOrNull() ?: ""
                    callback(firstName) // Devuelve el primer nombre a través del callback
                } else {
                    Log.d("HomeFragment", "El documento del usuario no existe.")
                    callback(null) // Devuelve null si no existe el documento
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error al obtener el nombre del usuario: ", exception)
                callback(null) // Devuelve null en caso de error
            }
    }

    private fun loadUserName() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: return

        getUserName(email) { firstName ->
            if (firstName != null) {
                userNameTextView.text = "$firstName, ¡Bienvenido a PerfectRep!"
            } else {
                userNameTextView.text = "Nombre no disponible" // Manejo de caso sin nombre
            }
        }
    }

}

