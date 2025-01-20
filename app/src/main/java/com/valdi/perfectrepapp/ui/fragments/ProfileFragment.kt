package com.valdi.perfectrepapp.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.ui.screens.authScreens.AuthActivity
import com.valdi.perfectrepapp.ui.screens.EditProfileActivity
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.utils.SessionDatesActivity

class ProfileFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val editProfileButton = view.findViewById<Button>(R.id.editProfileButton)

        val deleteSessionButton = view.findViewById<Button>(R.id.sessionsButton)

        editProfileButton.setOnClickListener {
            showEditProfile()
        }

        deleteSessionButton.setOnClickListener{
            showDeleteSession()
        }

        // Obtén el email del usuario desde SharedPreferences
        val email = activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
            ?.getString("email", null)

        if (email != null) {
            // Recupera los datos desde Firestore
            db.collection("users").document(email).get().addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name")
                    val height = document.getString("height")
                    val heightUnit = document.getString("heightUnit")
                    val weight = document.getString("weight")
                    val weightUnit = document.getString("weightUnit")
                    val profilePicture = document.getString("profile_picture")

                    Log.d("ProfileFragment", "email: $email, name: $name, height: $height, heightUnit: $heightUnit, weight: $weight, weightUnit: $weightUnit")

                    // Asigna los valores a los TextViews
                    view.findViewById<TextView>(R.id.emailTextViewProfile).text = "$email"
                    view.findViewById<TextView>(R.id.nameTextViewProfile).text = "$name"
                    view.findViewById<TextView>(R.id.heightTextViewProfile).text = "$height $heightUnit"
                    view.findViewById<TextView>(R.id.weightTextViewProfile).text = "$weight $weightUnit"

                    // Cargar la imagen del perfil si existe
                    val imageView = view.findViewById<ImageView>(R.id.imageViewPerfil)
                    if (profilePicture != null) {
                        Glide.with(this).load(profilePicture).into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.user_image) // Imagen por defecto
                    }
                } else {
                    Log.d("ProfileFragment", "No such document")
                }
            }.addOnFailureListener { exception ->
                Log.d("ProfileFragment", "get failed with ", exception)
            }
        } else {
            Log.d("ProfileFragment", "No email found in SharedPreferences")
        }

        // Configurar el botón de logout
        val logoutButton = view.findViewById<Button>(R.id.logOutButton)
        logoutButton.setOnClickListener {
            logOut()
        }
        return view
    }

    private fun logOut() {
        // Crear el AlertDialog
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Cerrar Sesión")
        builder.setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setCancelable(false)
            .setPositiveButton("Sí") { dialog, id ->
                // Si el usuario confirma, cerrar sesión
                // Borrar datos de SharedPreferences
                val prefs = activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)?.edit()
                prefs?.clear()
                prefs?.apply()

                // Cerrar sesión en Firebase
                FirebaseAuth.getInstance().signOut()

                // Redirigir al AuthActivity
                val intent = Intent(activity, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, id ->
                // Si el usuario cancela, simplemente cerrar el diálogo
                dialog.dismiss()
            }

        // Mostrar el AlertDialog
        val alert = builder.create()
        alert.show()
    }


    private fun showEditProfile() {
        val intent = Intent(requireContext(), EditProfileActivity::class.java)
        startActivity(intent)
    }

    private fun showDeleteSession(){
        val intent = Intent(requireContext(), SessionDatesActivity::class.java)
        startActivity(intent)
    }

    //Actualizar datos editados en el perfil
    override fun onResume() {
        super.onResume()
        // Vuelve a cargar la información del usuario
        val email = activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
            ?.getString("email", null)

        if (email != null) {
            db.collection("users").document(email).get().addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name")
                    val height = document.getString("height")
                    val heightUnit = document.getString("heightUnit")
                    val weight = document.getString("weight")
                    val weightUnit = document.getString("weightUnit")
                    val profilePicture = document.getString("profile_picture")

                    // Actualizar los TextViews con los nuevos datos
                    view?.findViewById<TextView>(R.id.nameTextViewProfile)?.text = name
                    view?.findViewById<TextView>(R.id.heightTextViewProfile)?.text = "$height $heightUnit"
                    view?.findViewById<TextView>(R.id.weightTextViewProfile)?.text = "$weight $weightUnit"

                    // Cargar la imagen del perfil actualizada
                    val imageView = view?.findViewById<ImageView>(R.id.imageViewPerfil)
                    if (profilePicture != null) {
                        if (imageView != null) {
                            Glide.with(this).load(profilePicture).into(imageView)
                        }
                    } else {
                        imageView?.setImageResource(R.drawable.user_image) // Imagen por defecto
                    }
                }
            }
        }
    }
}
