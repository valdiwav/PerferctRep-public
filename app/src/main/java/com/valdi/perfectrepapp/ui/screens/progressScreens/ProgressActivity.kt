package com.valdi.perfectrepapp.ui.screens.progressScreens

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.valdi.perfectrepapp.R
import java.util.*

class ProgressActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private val db = FirebaseFirestore.getInstance()
    private val errorsData = mutableMapOf<String, MutableMap<String, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        // Configuración de la ventana
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa componentes
        val exerciseTitleTextView = findViewById<TextView>(R.id.exerciseTitleTextView)
        lineChart = findViewById(R.id.lineChart)

        // Obtén el título del ejercicio
        val workoutTitle = intent.getStringExtra("exerciseTitle")
        exerciseTitleTextView.text = "Ejercicio: ${workoutTitle ?: "Título no encontrado"}"

        // Inicia la carga de datos y actualiza el gráfico
        loadErrorsForGraph(workoutTitle ?: "")


        // Encuentra el TextView para las recomendaciones
        val recommendationsTextView = findViewById<TextView>(R.id.recommendationsTextView)

        // Muestra las recomendaciones con imágenes y formato de acuerdo al título del ejercicio
        val recommendationsText = when (workoutTitle?.lowercase()) {
            "curl de bíceps" -> getString(R.string.tips_biceps_curl)
            "sentadillas" -> getString(R.string.tips_squat)
            "press de hombro" -> getString(R.string.tips_shoulder_press)
            "peso muerto" -> getString(R.string.tips_deadlift)
            else -> "¡Buena suerte y a entrenar!"
        }

        recommendationsTextView.text = Html.fromHtml(recommendationsText, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)


    }



    // Método de extensión para cargar texto con HTML y imágenes en TextView
    private fun TextView.setHtmlWithImages(html: String) {
        val imageGetter = Html.ImageGetter { source ->
            val resourceId = context.resources.getIdentifier(source, "drawable", context.packageName)
            val drawable = ContextCompat.getDrawable(context, resourceId)
            drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            drawable
        }
        val spanned: Spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
        this.text = spanned
    }

    private fun getImageGetter(): Html.ImageGetter {
        return Html.ImageGetter { source ->
            // Busca el recurso de imagen en drawable
            val resourceId = resources.getIdentifier(source, "drawable", packageName)
            val drawable = resources.getDrawable(resourceId, null)

            // Ajusta el tamaño de la imagen
            drawable?.setBounds(0, 0, 900, 600) // Ajusta el ancho y alto según el valor deseado
            drawable
        }
    }



    private fun loadErrorsForGraph(exerciseTitle: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        val email = user?.email ?: return

        Log.d("ProgressActivity", "Usuario: $email")

        // Ordenar por el ID del documento (que debería ser una fecha) en orden descendente para obtener los más recientes y limitar a 7
        db.collection("users").document(email)
            .collection("workout_sessions")
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING) // Orden descendente
            .limit(7) // Limitar a los últimos 7 documentos
            .get()
            .addOnSuccessListener { dateDocuments ->
                Log.d("ProgressActivity", "Se obtuvieron las sesiones correctamente. Total de sesiones: ${dateDocuments.size()}")

                errorsData.clear() // Limpiar datos de errores existentes

                if (dateDocuments.isEmpty) {
                    Log.d("ProgressActivity", "No se encontraron documentos de sesión.")
                    updateGraphWithErrorData()
                    return@addOnSuccessListener
                }

                val totalSessions = dateDocuments.size()
                var sessionsProcessed = 0

                for (dateDocument in dateDocuments) {
                    val date = dateDocument.id // El id del documento es la fecha
                    Log.d("ProgressActivity", "Procesando sesión en la fecha: $date")

                    var error1Sum = 0
                    var error2Sum = 0
                    var error3Sum = 0

                    // Obtener ejercicios de la sesión actual
                    db.collection("users").document(email)
                        .collection("workout_sessions").document(date)
                        .collection("exercises")
                        .whereEqualTo("exerciseTitle", exerciseTitle)
                        .get()
                        .addOnSuccessListener { exerciseDocuments ->
                            Log.d("ProgressActivity", "Se obtuvieron los ejercicios de la sesión $date. Total de ejercicios: ${exerciseDocuments.size()}")

                            if (exerciseDocuments.isEmpty) {
                                Log.d("ProgressActivity", "No se encontraron ejercicios en la sesión $date.")
                                sessionsProcessed++
                                checkAllSessionsProcessed(sessionsProcessed, totalSessions)
                                return@addOnSuccessListener
                            }




                            for (exerciseDocument in exerciseDocuments) {
                                // Obtener series dentro de cada ejercicio para sumar los errores
                                exerciseDocument.reference.collection("series")
                                    .get()
                                    .addOnSuccessListener { seriesDocuments ->
                                        for (serie in seriesDocuments) {
                                            val errorsMap = serie.get("errors") as? Map<String, Long> ?: continue
                                            if (exerciseTitle.equals("Curl de Bíceps", ignoreCase = true)) {
                                                error1Sum += errorsMap["elbowError"]?.toInt() ?: 0
                                                error2Sum += errorsMap["speedError"]?.toInt() ?: 0
                                                error3Sum += errorsMap["balanceError"]?.toInt() ?: 0
                                            }else if (exerciseTitle.equals("Sentadillas", ignoreCase = true)) {
                                                error1Sum += errorsMap["KneeErrorCountValue"]?.toInt() ?: 0
                                                error2Sum += errorsMap["legSpeedErrorCountValue"]?.toInt() ?: 0
                                                error3Sum += errorsMap["forceImbalanceErrorCountValue"]?.toInt() ?: 0
                                            }else if (exerciseTitle.equals("Press de Hombro", ignoreCase = true)) {
                                                error1Sum += errorsMap["shoulderElbowErrorValue"]?.toInt() ?: 0
                                                error2Sum += errorsMap["shoulderSpeedErrorValue"]?.toInt() ?: 0
                                                error3Sum += errorsMap["shoulderBalanceErrorValue"]?.toInt() ?: 0
                                            }else if (exerciseTitle.equals("Peso Muerto", ignoreCase = true)) {
                                                error1Sum += errorsMap["backErrorCount"]?.toInt() ?: 0
                                                error2Sum += errorsMap["deadLiftSpeedErrorValue"]?.toInt() ?: 0
                                                error3Sum += errorsMap["barDistanceErrorCount"]?.toInt() ?: 0
                                            }


                                        }

                                        // Guarda los errores acumulados para esta sesión
                                        errorsData[date] = mutableMapOf(
                                            "Postura" to error1Sum,
                                            "Velocidad" to error2Sum,
                                            "Ejecución" to error3Sum
                                        )



                                        sessionsProcessed++
                                        checkAllSessionsProcessed(sessionsProcessed, totalSessions)
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("ProgressActivity", "Error al obtener series en el ejercicio de la fecha $date: ", exception)
                                        sessionsProcessed++
                                        checkAllSessionsProcessed(sessionsProcessed, totalSessions)
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ProgressActivity", "Error al obtener ejercicios en la sesión $date: ", exception)
                            sessionsProcessed++
                            checkAllSessionsProcessed(sessionsProcessed, totalSessions)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProgressActivity", "Error al obtener sesiones de entrenamiento: ", exception)
            }
    }

    // Función para verificar que todas las sesiones hayan sido procesadas
    private fun checkAllSessionsProcessed(sessionsProcessed: Int, totalSessions: Int) {
        if (sessionsProcessed == totalSessions) {
            updateGraphWithErrorData()
        }
    }

    private fun updateGraphWithErrorData() {
        if (errorsData.isEmpty()) {
            Log.d("ProgressActivity", "No hay datos de errores para mostrar.")
            return
        }

        // Detectar si el sistema está en modo oscuro
        val isDarkTheme = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val elbowEntries = ArrayList<Entry>()
        val speedEntries = ArrayList<Entry>()
        val balanceEntries = ArrayList<Entry>()



        // Lista de fechas ordenadas para asegurar que las entradas sean consistentes
        val sortedDates = errorsData.keys.sorted()

        // Crear un mapa para las etiquetas del eje X
        val dateLabels = mutableListOf<String>()


        for ((index, date) in sortedDates.withIndex()) {
            // Cambiar el formato de la fecha para mostrar solo el día y el mes
            val parts = date.split("-")
            if (parts.size >= 3) {
                val formattedDate = "${parts[1]}-${parts[2]}" // "mes-día"
                dateLabels.add(formattedDate)
            }

            errorsData[date]?.let { errors ->
                elbowEntries.add(Entry(index.toFloat(), errors["Postura"]?.toFloat() ?: 0f))
                speedEntries.add(Entry(index.toFloat(), errors["Velocidad"]?.toFloat() ?: 0f))
                balanceEntries.add(Entry(index.toFloat(), errors["Ejecución"]?.toFloat() ?: 0f))
            }
        }


        val elbowDataSet = LineDataSet(elbowEntries, "Postura").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            circleRadius = 3f
            lineWidth = 1.5f
            valueTextColor = textColor // Color de texto de los valores

        }

        val speedDataSet = LineDataSet(speedEntries, "Velocidad").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            circleRadius = 3f
            lineWidth = 1.5f
            valueTextColor = textColor // Color de texto de los valores

        }

        val balanceDataSet = LineDataSet(balanceEntries, "Ejecución").apply {
            color = Color.GREEN
            setCircleColor(Color.GREEN)
            circleRadius = 3f
            lineWidth = 1.5f
            valueTextColor = textColor // Color de texto de los valores

        }

        val lineData = LineData(elbowDataSet, speedDataSet, balanceDataSet)
        lineChart.data = lineData

        lineChart.description.textColor = textColor
        lineChart.description.isEnabled = false

        // Configurar el eje X para mostrar las fechas como etiquetas
        lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dateLabels)
            granularity = 1f
            isGranularityEnabled = true
            textSize = 10f
            lineChart.xAxis.textColor = textColor
        }

        // Ajustar el zoom y la visibilidad
        lineChart.viewPortHandler.setMaximumScaleX(1f)
        lineChart.setVisibleXRangeMaximum(7f)
        // Configuración del eje Y
        lineChart.axisLeft.textColor = textColor
        lineChart.axisRight.textColor = textColor
        lineChart.legend.textColor = textColor


        lineChart.invalidate() // Actualizar el gráfico
    }




    private fun enableEdgeToEdge() {
        // Método de configuración adicional, puedes ajustarlo según necesites
    }
}
