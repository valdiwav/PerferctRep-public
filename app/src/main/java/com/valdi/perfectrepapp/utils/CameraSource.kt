package com.valdi.perfectrepapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import com.valdi.perfectrepapp.data.Person
import com.valdi.perfectrepapp.ml.PoseDetector
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraSource(
    private val surfaceView: SurfaceView,
    private val listener: CameraSourceListener? = null
) {

    companion object {
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480

        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f
        private const val TAG = "Camera Source"
    }



    private val lock = Any()
    private var detector: PoseDetector? = null
    private var isTrackerEnabled = false
    private var yuvConverter: YuvToRgbConverter = YuvToRgbConverter(surfaceView.context)
    private lateinit var imageBitmap: Bitmap

    /** Frame count that have been processed so far in an one second interval to calculate FPS. */
    private var fpsTimer: Timer? = null
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = surfaceView.context
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** Readers used as buffers for camera still shots */
    private var imageReader: ImageReader? = null

    /** The [CameraDevice] that will be opened in this fragment */
    private var camera: CameraDevice? = null

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private var session: CameraCaptureSession? = null

    /** [HandlerThread] where all buffer reading operations run */
    private var imageReaderThread: HandlerThread? = null

    /** [Handler] corresponding to [imageReaderThread] */
    private var imageReaderHandler: Handler? = null
    private var cameraId: String = ""

    suspend fun initCamera() {
        camera = openCamera(cameraManager, cameraId)
        imageReader =
            ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                if (!::imageBitmap.isInitialized) {
                    imageBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
                }
                yuvConverter.yuvToRgb(image, imageBitmap)

                // Obtener la rotación del dispositivo
                val windowManager = surfaceView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val rotation = windowManager.defaultDisplay.rotation

                // Crear matriz para rotar la imagen
                val rotateMatrix = Matrix()

                // Detectar si la cámara es frontal
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val isFrontFacing = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT

                // Ajustar la rotación basada en la orientación del dispositivo y si la cámara es frontal
                when (rotation) {
                    Surface.ROTATION_0 -> {
                        if (isFrontFacing) {
                            rotateMatrix.postRotate(270f)  // Ajuste especial para cámara frontal en vertical
                        } else {
                            rotateMatrix.postRotate(90f)  // Rotación normal para cámara trasera en vertical
                        }
                    }
                    Surface.ROTATION_90 -> rotateMatrix.postRotate(0f)   // Horizontal (izquierda)
                    Surface.ROTATION_180 -> rotateMatrix.postRotate(270f)  // Volteado vertical
                    Surface.ROTATION_270 -> rotateMatrix.postRotate(180f)  // Horizontal (derecha)
                }

                // Inversión horizontal si la cámara es frontal
                if (isFrontFacing) {
                    rotateMatrix.postScale(-1.0f, 1.0f)  // Inversión horizontal para cámara frontal
                }

                // Aplicar la rotación
                val rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT, rotateMatrix, false)

                processImage(rotatedBitmap)
                image.close()
            }
        }, imageReaderHandler)


        imageReader?.surface?.let { surface ->
            session = createSession(listOf(surface))
            val cameraRequest = camera?.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )?.apply {
                addTarget(surface)
            }
            cameraRequest?.build()?.let {
                session?.setRepeatingRequest(it, null, null)

            }
        }
    }




    private suspend fun createSession(targets: List<Surface>): CameraCaptureSession =
        suspendCancellableCoroutine { cont ->
            camera?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) =
                    cont.resume(captureSession)

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cont.resumeWithException(Exception("Session error"))
                }
            }, null)
        }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String): CameraDevice =
        suspendCancellableCoroutine { cont ->
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) = cont.resume(camera)

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    if (cont.isActive) cont.resumeWithException(Exception("Camera error"))
                }
            }, imageReaderHandler)
        }

    fun prepareCamera() {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)

            // Selecciona la cámara frontal
            if (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                this.cameraId = cameraId
                break  // Sal del bucle cuando encuentres la cámara frontal
            }
        }
    }


    fun setDetector(detector: PoseDetector) {
        synchronized(lock) {
            if (this.detector != null) {
                this.detector?.close()
                this.detector = null
            }
            this.detector = detector
        }
    }



    fun resume() {
        imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
        imageReaderHandler = Handler(imageReaderThread!!.looper)
        fpsTimer = Timer()
        fpsTimer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    fun close() {
        session?.close()
        session = null
        camera?.close()
        camera = null
        imageReader?.close()
        imageReader = null
        stopImageReaderThread()
        detector?.close()
        detector = null
        fpsTimer?.cancel()
        fpsTimer = null
        frameProcessedInOneSecondInterval = 0
        framesPerSecond = 0
    }

    // process image
    private fun processImage(bitmap: Bitmap) {
        val persons = mutableListOf<Person>()
        var classificationResult: List<Pair<String, Float>>? = null

        synchronized(lock) {
            detector?.estimatePoses(bitmap)?.let {
                persons.addAll(it)
            }
        }
        frameProcessedInOneSecondInterval++
        if (frameProcessedInOneSecondInterval == 1) {
            // send fps to view
            listener?.onFPSListener(framesPerSecond)
        }

        // if the model returns only one item, show that item's score.
        if (persons.isNotEmpty()) {
            listener?.onDetectedInfo(persons[0].score, classificationResult,persons)
        }
        visualize(persons, bitmap)
    }

    private fun visualize(persons: List<Person>, bitmap: Bitmap) {
        val outputBitmap = VisualizationUtils.drawBodyKeypoints(
            bitmap,
            persons.filter { it.score > MIN_CONFIDENCE }, isTrackerEnabled
        )

        val holder = surfaceView.holder
        val surfaceCanvas = holder.lockCanvas()
        surfaceCanvas?.let { canvas ->
            // Calcular la proporción de la pantalla y del bitmap
            val bitmapRatio = outputBitmap.width.toFloat() / outputBitmap.height.toFloat()
            val canvasRatio = canvas.width.toFloat() / canvas.height.toFloat()

            val left: Int
            val top: Int
            val right: Int
            val bottom: Int

            // Ajustar para que la imagen ocupe toda la pantalla y recortar las partes que sobrepasen
            if (bitmapRatio > canvasRatio) {
                // El bitmap es más ancho que la pantalla
                val scale = canvas.height.toFloat() / outputBitmap.height.toFloat()
                val newWidth = (outputBitmap.width * scale).toInt()
                left = (canvas.width - newWidth) / 2
                top = 0
                right = left + newWidth
                bottom = canvas.height
            } else {
                // El bitmap es más alto que la pantalla
                val scale = canvas.width.toFloat() / outputBitmap.width.toFloat()
                val newHeight = (outputBitmap.height * scale).toInt()
                left = 0
                top = (canvas.height - newHeight) / 2
                right = canvas.width
                bottom = top + newHeight
            }

            // Dibujar el bitmap escalado y recortado
            canvas.drawBitmap(
                outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
                Rect(left, top, right, bottom), null
            )

            surfaceView.holder.unlockCanvasAndPost(canvas)
        }
    }


    private fun stopImageReaderThread() {
        imageReaderThread?.quitSafely()
        try {
            imageReaderThread?.join()
            imageReaderThread = null
            imageReaderHandler = null
        } catch (e: InterruptedException) {
            Log.d(TAG, e.message.toString())
        }
    }

    interface CameraSourceListener {
        fun onFPSListener(fps: Int)

        fun onDetectedInfo(personScore: Float?, poseLabels: List<Pair<String, Float>>?,persons: List<Person>?)
    }
}