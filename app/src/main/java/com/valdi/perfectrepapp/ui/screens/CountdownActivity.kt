package com.valdi.perfectrepapp.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.valdi.perfectrepapp.R
import java.util.Locale

class CountdownActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvCountdown: TextView
    private var workoutTitle: String? = null
    private lateinit var imageExercise: ImageView
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        tvCountdown = findViewById(R.id.tvCountdown)
        imageExercise = findViewById(R.id.imageExercise)

        // Inicializar TTS
        tts = TextToSpeech(this, this)

        // Obtener los datos del intent
        workoutTitle = intent.getStringExtra("title")
        changeExerciseImage(workoutTitle)

        startCountdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun changeExerciseImage(title: String?) {
        when (title) {
            "Press de Hombro" -> imageExercise.setImageResource(R.drawable.press_de_hombro_image)
            "Curl de Bíceps" -> imageExercise.setImageResource(R.drawable.curl_de_biceps_image)
            "Sentadillas" -> imageExercise.setImageResource(R.drawable.sentadillas_image)
            "Peso Muerto" -> imageExercise.setImageResource(R.drawable.peso_muerto_image)
            else -> imageExercise.setImageResource(R.drawable.google24) // Imagen por defecto
        }
    }

    private fun startCountdown() {
        tvCountdown.visibility = View.VISIBLE

        val timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                tvCountdown.text = secondsLeft.toString()

                // Reproduce el texto solo en 3, 2 y 1
                if (secondsLeft in 1..3) {
                    speakOut(secondsLeft.toString())
                }
            }

            override fun onFinish() {
                tvCountdown.visibility = View.GONE
                val intent = Intent(this@CountdownActivity, PoseDetectionActivity::class.java)
                intent.putExtra("title", workoutTitle)
                startActivity(intent)
                finish()
            }
        }
        timer.start()
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        Toast.makeText(this, "Espera a que comience la serie", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // Liberar recursos de TTS al destruir la actividad
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
