package com.valdi.perfectrepapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.speech.tts.TextToSpeech
import com.valdi.perfectrepapp.R
import com.valdi.perfectrepapp.data.Device
import com.valdi.perfectrepapp.data.Person
import com.valdi.perfectrepapp.utils.CameraSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.valdi.perfectrepapp.ml.*
import com.valdi.perfectrepapp.ui.screens.feedbackScreens.SetFeedbackActivity
import com.valdi.perfectrepapp.utils.counters.BicepCurlCounter
import com.valdi.perfectrepapp.utils.counters.DeadLiftCounter
import com.valdi.perfectrepapp.utils.counters.ShoulderPressCounter
import com.valdi.perfectrepapp.utils.counters.SquatCounter
import java.util.Locale

class PoseDetectionActivity : AppCompatActivity(), BicepCurlCounter.BicepCurlCounterListener, SquatCounter.SquatCounterListener,ShoulderPressCounter.ShoulderPressCounterListener, DeadLiftCounter.DeadLiftCounterListener{

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var squatCounter : SquatCounter
    private lateinit var bicepCurlCounter: BicepCurlCounter
    private lateinit var shoulderPressCounter: ShoulderPressCounter
    private lateinit var deadLiftCounter: DeadLiftCounter


    object GlobalVariables {
        var elbowError: Boolean = false
    }

    private var workoutTitle: String? = null

    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    /** Default pose estimation model is 1 (MoveNet Thunder)
     * 0 == MoveNet Lightning model
     * 1 == MoveNet Thunder model
     * 2 == MoveNet MultiPose model
     * 3 == PoseNet model
     **/
    private var modelPos = 1

    /** Default device is CPU */
    private var device = Device.CPU
    private lateinit var tvScore: TextView
    private lateinit var tvFPS: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnModel: Spinner


    private lateinit var tvClassificationValue1: TextView
    private lateinit var tvClassificationValue2: TextView
    private lateinit var tvClassificationValue3: TextView

    private lateinit var tvRepCount: TextView
    private lateinit var tvErrorCount: TextView
    private lateinit var tvSpeedError: TextView

    private var cameraSource: CameraSource? = null
    private var isClassifyPose = false
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }
    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            changeModel(position)
        }
    }


    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }


    private var setClassificationListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            showClassificationResult(isChecked)
            isClassifyPose = isChecked
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pose_detection)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)
        tvFPS = findViewById(R.id.tvFps)
        spnModel = findViewById(R.id.spnModel)
        spnDevice = findViewById(R.id.spnDevice)
        surfaceView = findViewById(R.id.surfaceView)
        tvRepCount = findViewById(R.id.tvRepCount)
        tvErrorCount = findViewById(R.id.tvErrorCount)
        tvSpeedError = findViewById(R.id.tvSpeedError)

        requestPermission()

        // Inicializa TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }


        bicepCurlCounter = BicepCurlCounter(this)



        squatCounter = SquatCounter(this)


        shoulderPressCounter = ShoulderPressCounter(this)


        deadLiftCounter = DeadLiftCounter(this)

        initSpinner()
        spnModel.setSelection(modelPos)

        val endSetBtn = findViewById<Button>(R.id.endSetBtn)

        workoutTitle = intent.getStringExtra("title")




        endSetBtn.setOnClickListener{
            val setFeedBackIntent = Intent(this, SetFeedbackActivity::class.java).apply {
                if (workoutTitle != null){
                    putExtra("title", workoutTitle)

                    Log.d("PoseDetectionActivity", "Título del ejercicio: $workoutTitle")
                }else{
                    Log.d("PoseDetectionActivity", "Título no recibido")
                }

                //Retornar valor reps a setfeedback
                if (workoutTitle == "Curl de Bíceps"){


                    val repCountValue = bicepCurlCounter.getRepCount().toString()
                    putExtra("repCount", repCountValue)
                    Log.d("PoseDetectionActivity", "Conteo de repeticiones: $repCountValue") // Log para verificar el valor
                    val lastRIR = bicepCurlCounter.getLastRIR().toString()
                    putExtra("lastRIR",lastRIR)
                    Log.d("PoseDetectionActivity", "Conteo de repeticiones: $lastRIR") // Log para verificar el valor

                    val errorCount = bicepCurlCounter.getErrorCount().toString()
                    putExtra("errorCount",errorCount)

                    val speedError = bicepCurlCounter.getSpeedError().toString()
                    putExtra("speedError",speedError)

                    val balanceError = bicepCurlCounter.getBalanceError().toString()
                    putExtra("balanceError",balanceError)

                    val perfectRep = bicepCurlCounter.getPerfectRepCount().toString()
                    putExtra("perfectRep",perfectRep)

                    val errorRepCount = bicepCurlCounter.getErrorRepCount().toString()
                    putExtra("errorRepCount",errorRepCount)


                }else if (workoutTitle == "Sentadillas"){

                    val repCountValue = squatCounter.getRepCount().toString()
                    putExtra("repCount", repCountValue)
                    Log.d("PoseDetectionActivity", "Conteo de repeticiones: $repCountValue")
                    val kneeErrorCountValue = squatCounter.getKneeErrorCount().toString()
                    putExtra("KneeErrorCountValue", kneeErrorCountValue)
                    val forceImbalanceErrorCountValue = squatCounter.getKneeErrorCount().toString()
                    putExtra("forceImbalanceErrorCountValue", forceImbalanceErrorCountValue)
                    val legSpeedErrorCountValue = squatCounter.getSpeedError().toString()
                    putExtra("legSpeedErrorCountValue", legSpeedErrorCountValue)
                    val lastSquatsRIRValue = squatCounter.getLastRIR().toString()
                    putExtra("lastRIR", lastSquatsRIRValue)

                    val perfectRep = squatCounter.getPerfectRepCount().toString()
                    putExtra("perfectRep",perfectRep)

                    val errorRepCount = squatCounter.getErrorRepCount().toString()
                    putExtra("errorRepCount",errorRepCount)


                }else if (workoutTitle == "Press de Hombro"){
                    val repCountValue = shoulderPressCounter.getRepCount().toString()
                    putExtra("repCount", repCountValue)
                    val lastShoulderRIRValue = shoulderPressCounter.getLastRIR().toString()
                    putExtra("lastRIR", lastShoulderRIRValue)

                    val shoulderSpeedErrorValue = shoulderPressCounter.getShoulderSpeedErrorCount().toString()
                    putExtra("shoulderSpeedErrorValue", shoulderSpeedErrorValue)
                    val shoulderElbowErrorValue = shoulderPressCounter.getShoulderElbowErrorCount().toString()
                    putExtra("shoulderElbowErrorValue", shoulderElbowErrorValue)
                    val shoulderBalanceErrorValue = shoulderPressCounter.getShoulderBalanceErrorCount().toString()
                    putExtra("shoulderBalanceErrorValue", shoulderBalanceErrorValue)


                    val perfectRep = shoulderPressCounter.getPerfectRepCount().toString()
                    putExtra("perfectRep",perfectRep)

                    val errorRepCount = shoulderPressCounter.getErrorRepCount().toString()
                    putExtra("errorRepCount",errorRepCount)

                }else if (workoutTitle == "Peso Muerto"){
                    val repCountValue = deadLiftCounter.getRepCount().toString()
                    putExtra("repCount", repCountValue)
                    val lastDeadLiftRIRValue = deadLiftCounter.getLastRIR().toString()
                    putExtra("lastRIR", lastDeadLiftRIRValue)

                    val deadLiftSpeedErrorValue = deadLiftCounter.getSpeedError().toString()
                    putExtra("deadLiftSpeedErrorValue", deadLiftSpeedErrorValue)

                    val barDistanceErrorCount = deadLiftCounter.getBarDistanceError().toString()
                    putExtra("barDistanceErrorCount", barDistanceErrorCount)
                    val backErrorCount = deadLiftCounter.getBackError().toString()
                    putExtra("backErrorCount", backErrorCount)


                    val perfectRep = deadLiftCounter.getPerfectRepCount().toString()
                    putExtra("perfectRep",perfectRep)

                    val errorRepCount = deadLiftCounter.getErrorRepCount().toString()
                    putExtra("errorRepCount",errorRepCount)
                }

            }
            startActivity(setFeedBackIntent)
            finish()

        }

    }


    // Implementación del método de la interfaz
    override fun onIncorrectMovement(message: String) {
        // Reproduce el mensaje
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Implementación del método onRIRUpdated
    override fun onRIRUpdated(rir: Int) {
        // Puedes usar este valor para mostrar el RIR en la interfaz de usuario
        //Toast.makeText(this, "RIR aproximado: $rir", Toast.LENGTH_SHORT).show()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }


    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    // open camera
    private fun openCamera() {

        val bicep_curl = "Curl de Bíceps"
        val dead_lift = "Peso Muerto"
        val squats = "Sentadillas"
        val shoulder_press = "Press de Hombro"

        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?,
                            persons: List<Person>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)
                            poseLabels?.sortedByDescending { it.second }?.let {
                                tvClassificationValue1.text = getString(
                                    R.string.tfe_pe_tv_classification_value,
                                    convertPoseLabels(if (it.isNotEmpty()) it[0] else null)
                                )
                                tvClassificationValue2.text = getString(
                                    R.string.tfe_pe_tv_classification_value,
                                    convertPoseLabels(if (it.size >= 2) it[1] else null)
                                )
                                tvClassificationValue3.text = getString(
                                    R.string.tfe_pe_tv_classification_value,
                                    convertPoseLabels(if (it.size >= 3) it[2] else null)
                                )
                            }

                            //CONTEO DE REPETICIONES

                            // Aquí procesamos las personas detectadas para contar las repeticiones

                            if (workoutTitle == bicep_curl){

                                persons?.forEach { person ->
                                    bicepCurlCounter.updateRepCount(person)
                                }

                                // Actualizar el conteo de repeticiones en la UI
                                tvRepCount.text = "Reps: ${bicepCurlCounter.getRepCount()}"


                            }else if (workoutTitle == squats){

                                persons?.forEach { person ->
                                    squatCounter.updateRepCount(person)
                                }

                                // Actualizar el conteo de repeticiones en la UI
                                tvRepCount.text = "Reps: ${squatCounter.getRepCount()}"



                            }else if (workoutTitle == shoulder_press){

                                persons?.forEach { person ->
                                    shoulderPressCounter.updateRepCount(person)
                                }

                                // Actualizar el conteo de repeticiones en la UI
                                tvRepCount.text = "Reps: ${shoulderPressCounter.getRepCount()}"


                            }else if (workoutTitle == dead_lift){

                                persons?.forEach { person ->
                                    deadLiftCounter.updateRepCount(person)
                                }

                                // Actualizar el conteo de repeticiones en la UI
                                tvRepCount.text = "Reps: ${deadLiftCounter.getRepCount()}"



                            }
                        }
                    }).apply {
                        prepareCamera()
                    }
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }


    private fun convertPoseLabels(pair: Pair<String, Float>?): String {
        if (pair == null) return "empty"
        return "${pair.first} (${String.format("%.2f", pair.second)})"
    }



    // Initialize spinners to let user select model/accelerator/tracker.
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adaper ->
            adaper.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adaper
            spnDevice.onItemSelectedListener = changeDeviceListener
        }

    }

    // Change model when app is running
    private fun changeModel(position: Int) {
        if (modelPos == position) return
        modelPos = position
        createPoseEstimator()
    }

    // Change device (accelerator) type when app is running
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.GPU
            1 -> Device.CPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    private fun createPoseEstimator() {
        // For MoveNet MultiPose, hide score and disable pose classifier as the model returns
        // multiple Person instances.
        val poseDetector = when (modelPos) {
            0 -> {
                // MoveNet Lightning (SinglePose)
                showDetectionScore(true)
                MoveNet.create(this, device, ModelType.Lightning)
            }
            1 -> {
                // MoveNet Thunder (SinglePose)
                showDetectionScore(true)
                MoveNet.create(this, device, ModelType.Thunder)
            }
            else -> {
                null
            }
        }
        poseDetector?.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }


    // Show/hide the detection score.
    private fun showDetectionScore(isVisible: Boolean) {
        tvScore.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    // Show/hide classification result.
    private fun showClassificationResult(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        tvClassificationValue1.visibility = visibility
        tvClassificationValue2.visibility = visibility
        tvClassificationValue3.visibility = visibility
    }


    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        Toast.makeText(this, "Presiona FINALIZAR SERIE para salir ", Toast.LENGTH_SHORT).show()
    }

}