package com.valdi.perfectrepapp.ui.screens

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.valdi.perfectrepapp.R

class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_instructions)

        // Aplica el texto de instrucciones en formato HTML
        val tvInstructions: TextView = findViewById(R.id.tvInstructions)
        val instructionsHtml = getString(R.string.training_instructions)
        tvInstructions.text = HtmlCompat.fromHtml(instructionsHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)

        // Maneja los insets de sistema para soporte en pantalla completa
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
