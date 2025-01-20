package com.valdi.perfectrepapp.utils.counters

import com.valdi.perfectrepapp.data.BodyPart
import com.valdi.perfectrepapp.data.Person
import com.valdi.perfectrepapp.utils.VisualizationUtils
import kotlin.math.*

class DeadLiftCounter(private val listener: DeadLiftCounterListener) {

    private var previousPhase: String = "up"
    private var repCount: Int = 0
    private var speedError: Int = 0
    private var lastPhaseChangeTime: Long = 0

    private var forceImbalanceErrorCount: Int = 0
    private var backErrorCount: Int = 0
    private val minErrorInterval: Long = 2000 // Intervalo mínimo entre errores en milisegundos
    private var lastErrorTime: Long = 0

    private val repSpeeds = mutableListOf<Double>()
    private val calibrationReps = 3
    private var baseSpeed: Double? = null
    private var calibrated = false
    private var rirCount: Int = 4
    private var barDistanceErrorCount: Int = 0



    private var previousPoints: List<Pair<Float, Float>>? = null

    // Inicialización de la posición del peso muerto
    private var initialized: Boolean = false
    private val minHipAngleForInitialization = 150.0  // Ángulo mínimo para la posición de inicio

    private var eccentricStartTime: Long = 0
    private val minEccentricDuration = 1000 //milisegundos



    private val minHeadAngleThreshold = 20.0


    private val maxBarDistanceFromBodyX = 60.0  // Ajusta este valor según sea necesario

    // Función para verificar que la barra se mantenga cerca del cuerpo en el eje X
    private fun detectBarDistanceX(person: Person): Boolean {
        val leftWristX = person.keyPoints[BodyPart.LEFT_WRIST.position].coordinate.x
        val rightWristX = person.keyPoints[BodyPart.RIGHT_WRIST.position].coordinate.x
        val leftKneeX = person.keyPoints[BodyPart.LEFT_KNEE.position].coordinate.x
        val rightKneeX = person.keyPoints[BodyPart.RIGHT_KNEE.position].coordinate.x
        val leftAnkleX = person.keyPoints[BodyPart.LEFT_ANKLE.position].coordinate.x
        val rightAnkleX = person.keyPoints[BodyPart.RIGHT_ANKLE.position].coordinate.x

        // Verificar la distancia en X de las muñecas respecto a las rodillas y tobillos
        val leftWristDistanceX = min(abs(leftWristX - leftKneeX), abs(leftWristX - leftAnkleX))
        val rightWristDistanceX = min(abs(rightWristX - rightKneeX), abs(rightWristX - rightAnkleX))

        // Si alguna distancia en X es mayor al umbral, la barra está demasiado alejada
        return leftWristDistanceX > maxBarDistanceFromBodyX || rightWristDistanceX > maxBarDistanceFromBodyX
    }



    // Función para verificar la posición de la cabeza
    private fun detectHeadPosition(person: Person): Boolean {
        val nose = person.keyPoints[BodyPart.NOSE.position].coordinate
        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate

        // Calcular el punto medio entre los hombros para representar el torso
        val torsoCenter = Pair(
            (leftShoulder.x + rightShoulder.x) / 2,
            (leftShoulder.y + rightShoulder.y) / 2
        )

        // Calcular el ángulo entre la cabeza (nariz) y el torso
        val headAngle = calculateAngle(Pair(nose.x, nose.y), torsoCenter, Pair(torsoCenter.first, torsoCenter.second + 1))

        if (headAngle < minHeadAngleThreshold) {
            VisualizationUtils.isIncorrectTrunkMovement = true
            listener.onIncorrectMovement("Manten la espalda recta")
            return true
        }
        return false
    }



    private fun calculateAngle(p1: Pair<Float, Float>, p2: Pair<Float, Float>, p3: Pair<Float, Float>): Double {
        val angle = Math.toDegrees(
            atan2((p3.second - p2.second).toDouble(), (p3.first - p2.first).toDouble()) -
                    atan2((p1.second - p2.second).toDouble(), (p1.first - p2.first).toDouble())
        )
        return abs(angle).let { if (it > 180) 360 - it else it }
    }

    fun updateRepCount(person: Person) {

        VisualizationUtils.isIncorrectTrunkMovement = false
        VisualizationUtils.isIncorrectArmMovement = false



        val leftHip = person.keyPoints[BodyPart.LEFT_HIP.position].coordinate
        val rightHip = person.keyPoints[BodyPart.RIGHT_HIP.position].coordinate
        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate
        val leftKnee = person.keyPoints[BodyPart.LEFT_KNEE.position].coordinate
        val rightKnee = person.keyPoints[BodyPart.RIGHT_KNEE.position].coordinate

        val currentTime = System.currentTimeMillis()



        // Crear la lista de puntos clave actuales
        val currentPoints = listOf(
            Pair(leftHip.x, leftHip.y),
            Pair(rightHip.x, rightHip.y),
            Pair(leftKnee.x, leftKnee.y),
            Pair(leftKnee.x, rightKnee.y),
            Pair(leftShoulder.x, leftShoulder.y),
            Pair(rightShoulder.x, rightShoulder.y)
        )

        // Verificar si los puntos clave anteriores son nulos (primer fotograma)
        if (previousPoints == null) {
            previousPoints = currentPoints
            return // Salir de la función en el primer fotograma
        }

        // Usar el método areKeyPointsStable para verificar la estabilidad
        if (!areKeyPointsStable(previousPoints!!, currentPoints, 30f)) {
            println("Puntos clave inestables: no se procesará la repetición ni se detectarán errores")
            previousPoints = currentPoints  // Actualizar los puntos clave anteriores
            return
        }





        // Angulos para detectar el movimiento de peso muerto
        val hipAngle = calculateAngle(
            Pair(leftShoulder.x, leftShoulder.y),
            Pair(leftHip.x, leftHip.y),
            Pair(leftHip.x, leftHip.y + 1) // Aproximación para la alineación vertical
        )

        if (!initialized) {
            if (hipAngle > minHipAngleForInitialization) {
                initialized = true
            } else {
                return
            }
        }

        val startLiftThreshold = 120.0    // Ángulo para la fase de subida
        val fullStandThreshold = 150.0    // Ángulo para la posición de pie

        if (previousPoints == null) {
            previousPoints = listOf(
                Pair(leftHip.x, leftHip.y),
                Pair(rightHip.x, rightHip.y),
                Pair(leftShoulder.x, leftShoulder.y),
                Pair(rightShoulder.x, rightShoulder.y)
            )
            return
        }

        // Detección de distancia en X de la barra respecto al cuerpo solo si el ángulo de la cadera es menor a 120 grados
        if (hipAngle < 120.0 && detectBarDistanceX(person) && canCountError(currentTime)) {
            listener.onIncorrectMovement("Mantén la barra cerca")
            barDistanceErrorCount++
            VisualizationUtils.isIncorrectArmMovement = true
            lastErrorTime = currentTime
        }

        // Detección de posición de la cabeza
        if (detectHeadPosition(person) && canCountError(currentTime)) {
            backErrorCount++
            lastErrorTime = currentTime
        }


        if (previousPhase == "up") {
            if (hipAngle < startLiftThreshold) {
                previousPhase = "down"
                eccentricStartTime = currentTime
                lastPhaseChangeTime = currentTime
            }

        } else if (previousPhase == "down") {
            if (hipAngle > fullStandThreshold) {
                previousPhase = "up"
                repCount++

                val eccentricDuration = currentTime - eccentricStartTime
                if (eccentricDuration < minEccentricDuration) {
                    listener.onIncorrectMovement("Controla la bajada")
                    speedError++
                }

                val timeDifference = currentTime - lastPhaseChangeTime
                val speed = 1.0 / timeDifference * 1000
                repSpeeds.add(speed)

                if (repCount <= calibrationReps) {
                    baseSpeed = (baseSpeed?.times(repCount - 1) ?: (0.0 + speed)) / repCount
                    if (repCount == calibrationReps) calibrated = true
                }

                if (calibrated) {
                    rirCount = calculateRIR(speed)
                    listener.onRIRUpdated(rirCount)
                }

                lastPhaseChangeTime = currentTime
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

    private fun canCountError(currentTime: Long): Boolean {
        return currentTime - lastErrorTime > minErrorInterval
    }


    private fun areKeyPointsStable(previousPoints: List<Pair<Float, Float>>, currentPoints: List<Pair<Float, Float>>, threshold: Float): Boolean {
        for (i in previousPoints.indices) {
            val distance = sqrt(
                (currentPoints[i].first - previousPoints[i].first).pow(2) +
                        (currentPoints[i].second - previousPoints[i].second).pow(2)
            )
            if (distance > threshold) return false
        }
        return true
    }


    fun getRepCount(): Int {
        return repCount
    }

    fun getLastRIR(): Int {
        return rirCount
    }

    fun getSpeedError(): Int {
        return speedError
    }

    fun getBarDistanceError(): Int {
        return barDistanceErrorCount
    }

    fun getBackError(): Int {
        return backErrorCount
    }


    fun getErrorRepCount(): Int {
        return barDistanceErrorCount + speedError + backErrorCount
    }

    fun getPerfectRepCount(): Int {
        val perfectRep = repCount - getErrorRepCount()
        return if (perfectRep < 0) 0 else perfectRep
    }



    interface DeadLiftCounterListener {
        fun onRIRUpdated(rir: Int)
        fun onIncorrectMovement(message: String)
    }
}
