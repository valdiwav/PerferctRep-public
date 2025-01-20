package com.valdi.perfectrepapp.ui.screens.authScreens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.valdi.perfectrepapp.navigation.BottomNavActivity
import com.valdi.perfectrepapp.R

enum class ProviderType{
    BASIC,
    GOOGLE
}

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email?:"",provider?:"")

        //Guardado de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
        prefs.putString("email",email)
        prefs.putString("provider",provider)
        prefs.apply()
    }

    private fun setup(email:String,provider:String){
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val heightUnit = findViewById<Spinner>(R.id.heightUnitSpinner)
        val weightUnit = findViewById<Spinner>(R.id.weightUnitSpinner)
        val heightEditText = findViewById<EditText>(R.id.heightTextView)
        val weightEditText = findViewById<EditText>(R.id.weightTextView)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val getButton = findViewById<Button>(R.id.getButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val providerTextView = findViewById<TextView>(R.id.providerTextView)
        val logOutButton = findViewById<Button>(R.id.logOutButton)
        title = "Inicio"
        emailTextView.text = email
        providerTextView.text = provider

        logOutButton.setOnClickListener{

            //Borrar datos
            val prefs = getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            //Cerrar Sesión
            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        //Logica para guardar los datos del usuario (falta redirigirlo a la siguiente interface)
        saveButton.setOnClickListener {
            if (heightEditText.text.isNotEmpty() && weightEditText.text.isNotEmpty() && nameEditText.text.isNotEmpty()) {
                db.collection("users").document(email).set(
                    hashMapOf(
                        "provider" to provider,
                        "name" to nameEditText.text.toString(),
                        "height" to heightEditText.text.toString(),
                        "heightUnit" to heightUnit.selectedItem.toString(),
                        "weight" to weightEditText.text.toString(),
                        "weightUnit" to weightUnit.selectedItem.toString()
                    )
                )

                // Indicar en SharedPreferences que el perfil está completo
                val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
                prefs.putBoolean("profileComplete", true)
                prefs.apply()

                // Navegar a BottomNavActivity
                showHome(email, nameEditText.text.toString(), heightEditText.text.toString(), heightUnit.selectedItem.toString(), weightEditText.text.toString(), weightUnit.selectedItem.toString())
            } else {
                showAlert()
            }
        }


        //Logica para recuperar datos desde la db (uso unicamente de prueba)
        getButton.setOnClickListener{

            db.collection("users").document(email).get().addOnSuccessListener {

                nameEditText.setText(it.get("name") as String?)
                heightEditText.setText(it.get("height") as String?)

                // Obtiene el valor de heightUnit de Firestore
                val heightUnitValue = it.get("heightUnit") as String?

                // Obtiene el índice del valor en el Spinner y lo selecciona
                val heightUnitIndex = (heightUnit.adapter as ArrayAdapter<String>).getPosition(heightUnitValue)
                heightUnit.setSelection(heightUnitIndex)

                weightEditText.setText(it.get("weight") as String?)

                // Obtiene el valor de weightUnit de Firestore
                val weightUnitValue = it.get("weightUnit") as String?

                // Obtiene el índice del valor en el Spinner y lo selecciona
                val weightUnitIndex = (weightUnit.adapter as ArrayAdapter<String>).getPosition(weightUnitValue)
                weightUnit.setSelection(weightUnitIndex)
            }
        }

        //Logica para eliminar los datos ingresados ingresados en HomeActivity (uso unicamente de prueba)
        deleteButton.setOnClickListener{

            db.collection("users").document(email).delete()
        }
    }

    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Debe completar todos los campos para continuar")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, name: String, height: String, heightUnit: String, weight: String, weightUnit: String) {
        val intent = Intent(this, BottomNavActivity::class.java).apply {
            putExtra("email", email)
            putExtra("name", name)
            putExtra("height", height)
            putExtra("heightUnit", heightUnit)
            putExtra("weight", weight)
            putExtra("weightUnit", weightUnit)
        }
        startActivity(intent)
        finish()
    }




}