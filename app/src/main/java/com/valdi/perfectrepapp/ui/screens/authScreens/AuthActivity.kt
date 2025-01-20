package com.valdi.perfectrepapp.ui.screens.authScreens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.navigation.BottomNavActivity
import com.valdi.perfectrepapp.R

class AuthActivity : AppCompatActivity() {
    private val GOOGLE_SIGN_IN = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup
        setup()
        sesion()
    }

    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)
        val authLayout = findViewById<RelativeLayout>(R.id.authLayout)
        if (email != null && provider != null) {
            authLayout.visibility = View.INVISIBLE
        }else{
            authLayout.visibility = View.VISIBLE
        }
    }

    private fun sesion() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)
        val authLayout = findViewById<RelativeLayout>(R.id.authLayout)

        if (email != null && provider != null) {
            authLayout.visibility = View.INVISIBLE

            // Verificamos si los datos del perfil están completos en la base de datos
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(email).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val height = document.getString("height")
                    val weight = document.getString("weight")

                    if (!name.isNullOrEmpty() && !height.isNullOrEmpty() && !weight.isNullOrEmpty()) {
                        // Si el perfil está completo, redirigir a BottomNavActivity
                        showHome(email, ProviderType.valueOf(provider))
                    } else {
                        // Si el perfil no está completo, redirigir a HomeActivity
                        showSignIn(email, ProviderType.valueOf(provider))
                    }
                } else {
                    // Documento no existe, redirigir a HomeActivity
                    showSignIn(email, ProviderType.valueOf(provider))
                }
            }.addOnFailureListener {
                // Manejo de error, redirigir a HomeActivity
                showSignIn(email, ProviderType.valueOf(provider))
            }
        } else {
            // Si no hay email y provider en SharedPreferences, mostrar pantalla de autenticación
            authLayout.visibility = View.VISIBLE
        }
    }



    private fun setup() {
        title = "Autentificación"
        val signUpButton = findViewById<TextView>(R.id.signIn)
        val logInButton = findViewById<Button>(R.id.logInButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val googleButton = findViewById<Button>(R.id.googleButton)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)

        // Lógica botón REGISTRAR
        signUpButton.setOnClickListener {
            val signInActivityIntent = Intent(this, SignInActivity::class.java)
            startActivity(signInActivityIntent)
        }

        // Lógica botón ACCEDER
        logInButton.setOnClickListener {
            val emailInput = emailEditText.text.toString()
            val passwordInput = passwordEditText.text.toString()

            // Validación de campos vacíos
            when {
                emailInput.isEmpty() -> {
                    showAlert("Por favor, ingrese su correo electrónico.")
                    return@setOnClickListener
                }
                passwordInput.isEmpty() -> {
                    showAlert("Por favor, ingrese su contraseña.")
                    return@setOnClickListener
                }
            }

            // Intentar iniciar sesión
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Inicio de sesión exitoso
                        val userEmail = task.result?.user?.email ?: ""
                        val db = FirebaseFirestore.getInstance()
                        val userDocRef = db.collection("users").document(userEmail)

                        // Verificar si el documento del usuario ya existe
                        userDocRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Si el documento existe, redirigir a BottomNavActivity
                                showHome(userEmail, ProviderType.BASIC)
                            } else {
                                // Si el documento no existe, redirigir a HomeActivity para completar perfil
                                showSignIn(userEmail, ProviderType.BASIC)
                            }
                        }.addOnFailureListener { e ->
                            // Manejo de errores al intentar acceder a Firestore
                            showAlert("Error al verificar el usuario: ${e.message ?: "Error desconocido."}")
                        }
                    } else {
                        // Manejo de errores en el inicio de sesión
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> "No hay un usuario registrado con este correo."
                            is FirebaseAuthInvalidCredentialsException -> "La contraseña es incorrecta."
                            else -> "Error al iniciar sesión: ${task.exception?.localizedMessage ?: "Error desconocido."}"
                        }
                        showAlert(errorMessage)
                    }
                }

        }


        googleButton.setOnClickListener {
            // Configuración
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }

        forgotPassword.setOnClickListener {
            val forgotPasswordIntent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(forgotPasswordIntent)
        }
    }

    private fun showAlert(message: String = "Se ha producido un error autenticando al usuario") {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showSignIn(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val email = account.email ?: ""
                            val db = FirebaseFirestore.getInstance()
                            val userDocRef = db.collection("users").document(email)

                            userDocRef.get().addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // Usuario ya existe en Firestore, redirige al BottomNav
                                    showHome(email, ProviderType.GOOGLE)
                                } else {
                                    // Usuario no existe en Firestore, redirige a HomeActivity para completar el perfil
                                    showSignIn(email, ProviderType.GOOGLE)
                                }
                            }.addOnFailureListener { e ->
                                showAlert("Error al verificar el usuario: ${e.message}")
                            }
                        } else {
                            it.exception?.let { e ->
                                showAlert("Error: ${e.message}")
                            } ?: showAlert("Se ha producido un error desconocido.")
                        }
                    }
                }
            } catch (e: ApiException) {
                showAlert("Error: ApiException ${e.statusCode}")
            }
        }
    }

    private fun showHome(email: String, provider: ProviderType) {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider.name)
        prefs.apply()
        finish()

        val homeIntent = Intent(this, BottomNavActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
        finish()
    }
}
