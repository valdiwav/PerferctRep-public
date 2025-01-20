package com.valdi.perfectrepapp.ui.screens

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.databinding.ActivityEditProfileBinding
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        loadInfo()

        // Selección de imagen
        binding.changeProfilePicture.setOnClickListener {
            selectImageFromGallery()
        }

        binding.updateProfileData.setOnClickListener {
            val email = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
                .getString("email", null)

            val updatedName = binding.nameEditTextProfile.text.toString()
            val updatedHeight = binding.heiEditTextProfile.text.toString()
            val updatedWeight = binding.weightEditTextProfile.text.toString()

            if (updatedName.isEmpty() || updatedHeight.isEmpty() || updatedWeight.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mostrar el diálogo de confirmación
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmar cambios")
            builder.setMessage("¿Estás seguro de que quieres realizar los cambios?")
            builder.setPositiveButton("Sí") { _, _ ->
                if (email != null) {
                    progressDialog.show()

                    // Si se seleccionó una nueva imagen, subirla a Firebase Storage
                    if (selectedImageUri != null) {
                        uploadProfilePicture(email) { imageUrl ->
                            updateProfileData(email, updatedName, updatedHeight, updatedWeight, imageUrl)
                        }
                    } else {
                        updateProfileData(email, updatedName, updatedHeight, updatedWeight, null)
                    }
                }
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Cierra el diálogo sin hacer nada
            }

            // Mostrar el diálogo
            builder.show()
        }


        binding.cancelButtonProfile.setOnClickListener {
            finish()
        }
    }

    // Método para seleccionar imagen desde la galería
    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    // Manejar el resultado de la selección de imagen
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data!!.data
            binding.profileImageView.setImageURI(selectedImageUri)
        }
    }

    // Método para subir la imagen a Firebase Storage
    private fun uploadProfilePicture(email: String, callback: (imageUrl: String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$email/${UUID.randomUUID()}.jpg")
        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        callback(downloadUri.toString())  // Devuelve la URL de la imagen subida
                    }
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Método para actualizar los datos del perfil en Firestore
    private fun updateProfileData(email: String, name: String, height: String, weight: String, imageUrl: String?) {
        val db = FirebaseFirestore.getInstance()
        val updateData = mutableMapOf<String, Any>(
            "name" to name,
            "height" to height,
            "weight" to weight
        )

        imageUrl?.let {
            updateData["profile_picture"] = it  // Actualizar la URL de la imagen si se ha subido una nueva
        }

        db.collection("users").document(email).update(updateData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                finish()  // Vuelve a la pantalla anterior
            }.addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al actualizar el perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadInfo() {
        val email = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
            .getString("email", null)

        if (email == null) {
            Toast.makeText(this, "Error al obtener el email del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                progressDialog.dismiss()
                val name = document.getString("name")
                val height = document.getString("height")
                val weight = document.getString("weight")
                val profilePicture = document.getString("profile_picture")

                binding.nameEditTextProfile.setText(name)
                binding.heiEditTextProfile.setText(height)
                binding.weightEditTextProfile.setText(weight)

                // Cargar la imagen del perfil si existe
                if (profilePicture != null) {
                    Glide.with(this).load(profilePicture).into(binding.profileImageView)
                } else {
                    binding.profileImageView.setImageResource(R.drawable.user_image)
                }
            }.addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al cargar los datos del perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
