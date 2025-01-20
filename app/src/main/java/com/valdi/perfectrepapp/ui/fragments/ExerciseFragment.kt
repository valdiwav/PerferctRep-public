package com.valdi.perfectrepapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.utils.adapters.ExerciseListAdapter

class ExerciseFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private val imageUrls = mutableListOf<String>()
    private val titles = mutableListOf<String>()
    private val descriptions = mutableListOf<String>()
    private val videoUrls = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_excercise, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        firestore = FirebaseFirestore.getInstance()

        // Obtener todos los documentos de la colección "exercises"
        firestore.collection("exercises").get().addOnSuccessListener { querySnapshot ->
            imageUrls.clear()
            titles.clear()
            descriptions.clear()
            videoUrls.clear()

            for (document in querySnapshot.documents) {
                // Extraer los datos directamente de Firestore
                val imageUrl = document.getString("imageUrl") ?: ""
                val exerciseName = document.getString("exercise_name") ?: "Unknown Title"
                val description = document.getString("description") ?: "No description available"
                val videoUrl = document.getString("videoUrl") ?: ""

                imageUrls.add(imageUrl)
                titles.add(exerciseName) // Ahora usamos "exercise_name" en vez de extraerlo del archivo
                descriptions.add(description)
                videoUrls.add(videoUrl)
            }

            // Notificar al adaptador que los datos han cambiado
            recyclerView.adapter?.notifyDataSetChanged()
        }

        // Configurar el adaptador con las listas de datos
        recyclerView.adapter = ExerciseListAdapter(imageUrls, titles, descriptions, videoUrls) { imageUrl ->
            // Manejar el clic en la imagen
        }

        return view
    }
}
