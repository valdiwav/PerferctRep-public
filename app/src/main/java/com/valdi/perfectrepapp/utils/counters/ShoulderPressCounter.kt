package com.valdi.perfectrepapp.utils.counters

import com.valdi.perfectrepapp.data.BodyPart
import com.valdi.perfectrepapp.data.Person
import com.valdi.perfectrepapp.utils.VisualizationUtils
import kotlin.math.abs
import kotlin.math.atan2

class ShoulderPressCounter(private val listener: ShoulderPressCounterListener) {

    private var previousPhase: String = "down"
    private var repCount: Int = 0
    private var lastPhaseChangeTime: Long = 0
    private var initialized: Boolean = false
    // Variables para el cálculo de RIR
    private val repSpeeds = mutableListOf<Double>()
    private val calibrationReps = 3
    private var baseSpeed: Double? = null
    private var calibrated = false
    private var rirCount: Int = 4
    private var speedError: Int = 0

    // Umbral para detectar codos separados (en píxeles o unidades que se ajusten)
    private val maxLateralElbowDistance = 80.0  // Ajusta este valor según pruebas y dimensiones de la imagen
    // Umbral para detectar diferencia de altura entre muñecas (en píxeles o unidades que se ajusten)
    private val maxWristHeightDifference = 15.0  // Ajusta este valor según pruebas
    private val maxShoulderElbowProjectionAngle = 80.0

    private val minErrorInterval: Long = 2000 // Intervalo mínimo entre errores en milisegundos
    private var errorAlreadyCounted: Boolean = false
    private var lastErrorTime: Long = 0 // Marca de tiempo del último error contado
    private var shoulderElbowErrorCount: Int = 0
    private var shoulderBalanceErrorCount: Int = 0




    // Umbrales y límites
    private val shoulderPressTopThreshold = 0.5
    private val minElbowAngle = 70.0
    private val maxElbowAngle = 160.0
    private val minWristHeightDifference = 20.0  // Diferencia de altura mínima entre muñeca y hombro para la fase de subida

    fun updateRepCount(person: Person) {

        VisualizationUtils.isIncorrectShoulderMovement = false
        VisualizationUtils.isIncorrectBalanceShoulderMovement = false
        VisualizationUtils.isIncorrectSpeedShoulderMovement = false



        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate
        val leftElbow = person.keyPoints[BodyPart.LEFT_ELBOW.position].coordinate
        val rightElbow = person.keyPoints[BodyPart.RIGHT_ELBOW.position].coordinate
        val leftWrist = person.keyPoints[BodyPart.LEFT_WRIST.position].coordinate
        val rightWrist = person.keyPoints[BodyPart.RIGHT_WRIST.position].coordinate
        val leftHip = person.keyPoints[BodyPart.LEFT_HIP.position].coordinate
        val rightHip = person.keyPoints[BodyPart.RIGHT_HIP.position].coordinate

        // Verificar visibilidad de puntos clave
        if (!allKeyPointsVisible(person, listOf(
                BodyPart.LEFT_SHOULDER.position, BodyPart.RIGHT_SHOULDER.position,
                BodyPart.LEFT_ELBOW.position, BodyPart.RIGHT_ELBOW.position,
                BodyPart.LEFT_WRIST.position, BodyPart.RIGHT_WRIST.position
            ))) {
            println("No todos los puntos clave están visibles: pausa en el conteo")
            return
        }

        val currentTime = System.currentTimeMillis()

        // Verificar si ha pasado el intervalo mínimo desde el último error
        val canCountError = currentTime - lastErrorTime > minErrorInterval

        // Detectar ángulos de los codos
        val leftElbowAngle = calculateAngle(
            Pair(leftShoulder.x, leftShoulder.y),
            Pair(leftElbow.x, leftElbow.y),
            Pair(leftWrist.x, leftWrist.y)
        )
        val rightElbowAngle = calculateAngle(
            Pair(rightShoulder.x, rightShoulder.y),
            Pair(rightElbow.x, rightElbow.y),
            Pair(rightWrist.x, rightWrist.y)
        )

        // Cálculo del ángulo cadera-hombro-codo
        val leftShoulderElbowAngle = calculateAngle(
            Pair(leftHip.x, leftHip.y),
            Pair(leftShoulder.x, leftShoulder.y),
            Pair(leftElbow.x, leftElbow.y)
        )
        val rightShoulderElbowAngle = calculateAngle(
            Pair(rightHip.x, rightHip.y),
            Pair(rightShoulder.x, rightShoulder.y),
            Pair(rightElbow.x, rightElbow.y)
        )


        // Cálculo de la distancia lateral (en el eje X) entre el hombro y el codo
        val leftElbowLateralDistance = abs(leftShoulder.x - leftElbow.x)
        val rightElbowLateralDistance = abs(rightShoulder.x - rightElbow.x)

        errorAlreadyCounted = false  // Resetear error para nueva fase

        // Comprobación de codos muy separados
        if (leftElbowLateralDistance > maxLateralElbowDistance || rightElbowLateralDistance > maxLateralElbowDistance) {
            println("Error: Codos demasiado separados")
            listener.onIncorrectMovement("Lleva lo codos al torso")
            VisualizationUtils.isIncorrectShoulderMovement = true

            if (canCountError) {
                shoulderElbowErrorCount++
                lastErrorTime = currentTime
                errorAlreadyCounted = false // Permitir que el error se cuente nuevamente
            }
        }


        // Calcular la diferencia de altura entre las muñecas
        val wristHeightDifference = abs(leftWrist.y - rightWrist.y)


        if (leftShoulderElbowAngle > maxShoulderElbowProjectionAngle && rightShoulderElbowAngle > maxShoulderElbowProjectionAngle){
            // Detectar si hay balanceo de fuerza
            if (wristHeightDifference > maxWristHeightDifference) {
                println("Error de balanceo de fuerza detectado: un brazo se eleva más que el otro")
                listener.onIncorrectMovement("Evita balancearte")
                VisualizationUtils.isIncorrectBalanceShoulderMovement = true
                if (canCountError) {
                    shoulderBalanceErrorCount++
                    lastErrorTime = currentTime
                    errorAlreadyCounted = false // Permitir que el error se cuente nuevamente
                }
            }
        }


        // Detectar posición inicial
        if (!initialized) {
            if (leftElbowAngle < maxElbowAngle && rightElbowAngle < maxElbowAngle &&
                leftWrist.y > leftShoulder.y && rightWrist.y > rightShoulder.y) {
                initialized = true
                println("Posición inicial detectada para el press de hombro.")
            } else {
                println("Esperando que el usuario esté en posición inicial...")
                return
            }
        }

        // Fase de bajada (codos flexionados y muñecas debajo del nivel de los hombros)
        if (previousPhase == "up") {
            if (leftElbowAngle < minElbowAngle && rightElbowAngle < minElbowAngle &&
                leftWrist.y > leftShoulder.y && rightWrist.y > rightShoulder.y) {
                previousPhase = "down"
                println("Fase de bajada detectada.")

                // Medir el tiempo de la fase de bajada
                val downPhaseTime = currentTime - lastPhaseChangeTime

                // Si el tiempo de bajada es menor a 1.5 segundos, incrementar speedError
                if (downPhaseTime < 1500) {
                    speedError++
                    println("Error de velocidad en la fase de bajada: $speedError")
                    listener.onIncorrectMovement("Baja mas lento")
                    VisualizationUtils.isIncorrectSpeedShoulderMovement = false
                }



                lastPhaseChangeTime = System.currentTimeMillis()
            }


        } else if (previousPhase == "down") {
            // Fase de subida: muñecas por encima del nivel de los hombros y codos extendidos
            if (leftElbowAngle > maxElbowAngle && rightElbowAngle > maxElbowAngle &&
                leftWrist.y < (leftShoulder.y - minWristHeightDifference) &&
                rightWrist.y < (rightShoulder.y - minWristHeightDifference)) {
                previousPhase = "up"
                repCount++
                println("Repetición completada: $repCount")

                // Calcular la velocidad de subida SOLO durante la fase de subida
                val timeDifference = currentTime - lastPhaseChangeTime
                val speed = 1.0 / timeDifference * 1000  // Velocidad en repeticiones por segundo
                repSpeeds.add(speed)

                // Calibración de la velocidad base usando las primeras repeticiones
                if (repCount <= calibrationReps) {
                    baseSpeed = (baseSpeed?.times(repCount - 1) ?: (0.0 + speed)) / repCount
                    if (repCount == calibrationReps) calibrated = true
                }

                // Ajuste de baseSpeed y cálculo del RIR solo después de la fase de calibración
                if (calibrated) {
                    val alpha = 0.2  // Factor de suavizado para el cálculo de velocidad
                    baseSpeed = baseSpeed?.let { (1 - alpha) * it + alpha * speed } ?: speed
                    rirCount = calculateRIR(speed)
                    listener.onRIRUpdated(rirCount)
                }


                lastPhaseChangeTime = System.currentTimeMillis()
            }
        }
    }

    private fun calculateRIR(currentSpeed: Double): Int {
        val base = baseSpeed ?: return 4
        val speedRatio = currentSpeed / base

        return when {
            speedRatio >= 1 -> 4
            speedRatio > 0.9 -> 3 // Velocidad cae menos del 10%, RIR 3
            speedRatio > 0.8 -> 2 // Velocidad cae entre 10-20%, RIR 2
            speedRatio > 0.7 -> 1 // Velocidad cae entre 20-30%, RIR 1
            else -> 0          // Velocidad cae más del 30%, RIR 0
        }
    }

    private fun calculateAngle(p1: Pair<Float, Float>, p2: Pair<Float, Float>, p3: Pair<Float, Float>): Double {
        val angle = Math.toDegrees(
            atan2((p3.second - p2.second).toDouble(), (p3.first - p2.first).toDouble()) -
                    atan2((p1.second - p2.second).toDouble(), (p1.first - p2.first).toDouble())
        )
        return abs(angle).let { if (it > 180) 360 - it else it }
    }

    private fun allKeyPointsVisible(person: Person, requiredPoints: List<Int>): Boolean {
        for (index in requiredPoints) {
            val keyPoint = person.keyPoints[index]
            if (keyPoint.score < 0.5) {
                return false
            }
        }
        return true
    }

    fun getRepCount(): Int {
        return repCount
    }

    fun getLastRIR(): Int {
        return rirCount
    }

    fun getShoulderSpeedErrorCount(): Int {
        return speedError
    }

    fun getShoulderElbowErrorCount(): Int {
        return shoulderElbowErrorCount
    }

    fun getShoulderBalanceErrorCount(): Int {
        return shoulderBalanceErrorCount
    }


    fun getErrorRepCount(): Int {
        return shoulderElbowErrorCount + speedError + shoulderBalanceErrorCount
    }

    fun getPerfectRepCount(): Int {
        val perfectRep = repCount - getErrorRepCount()
        return if (perfectRep < 0) 0 else perfectRep
    }



    interface ShoulderPressCounterListener {
        fun onRIRUpdated(rir: Int)
        fun onIncorrectMovement(message: String)
    }
}
