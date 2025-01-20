package com.valdi.perfectrepapp.ui.fragments

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.valdi.perfectrepapp.R

class InstructionsFragment : Fragment() {

    override fun onCreateView(
        
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el layout para el fragmento
        val view = inflater.inflate(R.layout.fragment_instructions, container, false)

        // Configurar el TextView con el texto de instrucciones en HTML y ImageGetter
        val tvInstructions: TextView = view.findViewById(R.id.tvInstructions)
        val instructionsHtml = getString(R.string.training_instructions)

        tvInstructions.text = HtmlCompat.fromHtml(
            instructionsHtml,
            HtmlCompat.FROM_HTML_MODE_LEGACY,
            ImageGetter(requireContext()), // Pasar el contexto aquí
            null
        )

        return view
    }

    // Implementación de ImageGetter para cargar imágenes desde drawable
    private class ImageGetter(private val context: Context) : Html.ImageGetter {
        override fun getDrawable(source: String): Drawable? {
            return try {
                val resId = context.resources.getIdentifier(source, "drawable", context.packageName)
                val drawable = ContextCompat.getDrawable(context, resId)
                drawable?.apply {
                    // Configurar tamaño de imagen si no es nulo
                    setBounds(0, 0, 1000, 600)
                } ?: run {
                    Log.e("ImageGetter", "Error: Imagen '$source' no encontrada en drawable.")
                    null
                }
            } catch (e: Exception) {
                Log.e("ImageGetter", "Error al cargar la imagen '$source': ${e.message}")
                null
            }
        }
    }
}
