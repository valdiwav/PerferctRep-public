package com.valdi.perfectrepapp.ui.screens.authScreens

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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

class SignInActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los EditText y Button
        val newEmail = findViewById<EditText>(R.id.emailEditText)
        val newPassword = findViewById<EditText>(R.id.passwordEditText)
        val confirmPassword = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signInButton = findViewById<Button>(R.id.signInButton)

        signInButton.setOnClickListener {
            val email = newEmail.text.toString().trim()
            val pass1 = newPassword.text.toString().trim()
            val pass2 = confirmPassword.text.toString().trim()

            // Comprobar que los campos no están vacíos
            if (email.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
                Toast.makeText(baseContext, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar formato de correo
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(baseContext, "Por favor, ingresa un correo válido", Toast.LENGTH_SHORT).show()
                newEmail.requestFocus()
                return@setOnClickListener
            }

            // Verificar que las contraseñas coincidan
            if (pass1 != pass2) {
                Toast.makeText(baseContext, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                newPassword.requestFocus()
                return@setOnClickListener
            }

            // Validar complejidad de la contraseña
            if (!isPasswordValid(pass1)) {
                Toast.makeText(baseContext, "La contraseña debe tener al menos 8 caracteres.", Toast.LENGTH_LONG).show()
                Toast.makeText(baseContext, "Usa al menos una mayúscula, número y un carácter especial.", Toast.LENGTH_LONG).show()
                newPassword.requestFocus()
                return@setOnClickListener
            }

            // Si todas las validaciones pasan, crear la cuenta
            createAccount(email, pass1)
        }

        firebaseAuth = Firebase.auth
    }

    // Método para crear la cuenta en Firebase Authentication
    private fun createAccount(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Cuenta creada exitosamente, redirigir a AuthActivity
                    Toast.makeText(baseContext, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                    val authActivityIntent = Intent(this, AuthActivity::class.java)
                    startActivity(authActivityIntent)
                    finish() // Finalizar la actividad actual para que el usuario no pueda volver atrás
                } else {
                    // Si la creación de la cuenta falla, mostrar un mensaje de error
                    Toast.makeText(baseContext, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Validar que la contraseña cumpla con los requisitos de seguridad
    private fun isPasswordValid(password: String): Boolean {
        // Patrón actualizado: al menos una letra minúscula, una letra mayúscula, un número y un carácter especial cualquiera, mínimo 8 caracteres
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d\\S]{8,}\$")
        return passwordPattern.matches(password)
    }

}
