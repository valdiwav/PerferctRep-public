package com.valdi.perfectrepapp.ui.screens.authScreens

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.valdi.perfectrepapp.R

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val forgotPasswordEmail = findViewById<EditText>(R.id.emailEditText)
        val btnChangePassword = findViewById<Button>(R.id.restorePasswordButton)

        // Inicializar FirebaseAuth
        firebaseAuth = Firebase.auth

        btnChangePassword.setOnClickListener {
            // Verificar si el campo de correo electrónico no está vacío
            val email = forgotPasswordEmail.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tu correo electrónico.", Toast.LENGTH_SHORT).show()
            } else {
                // Si el correo no está vacío, enviar el correo de restablecimiento
                sendPasswordReset(email)
                val authActivityIntent = Intent(this, AuthActivity::class.java)
                startActivity(authActivityIntent)
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(baseContext, "Correo de cambio enviado!", Toast.LENGTH_SHORT).show()


                } else {
                    Toast.makeText(baseContext, "El correo ingresado no existe", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
