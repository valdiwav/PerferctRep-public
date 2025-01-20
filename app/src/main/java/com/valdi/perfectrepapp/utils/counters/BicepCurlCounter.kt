package com.valdi.perfectrepapp.utils.counters

import android.graphics.PointF
import com.valdi.perfectrepapp.data.BodyPart
import com.valdi.perfectrepapp.data.Person
import com.valdi.perfectrepapp.ui.screens.PoseDetectionActivity
import com.valdi.perfectrepapp.utils.VisualizationUtils
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class BicepCurlCounter(private val listener: BicepCurlCounterListener) {

    private var previousPhaseLeft: String = "down"
    private var previousPhaseRight: String = "down"
    private var repCount: Int = 0
    private var rirCount: Int = 4
    private var lastPhaseChangeTime: Long = 0 // Para el cálculo de la velocidad

    private var elbowErrorCount: Int = 0 // Contador de errores específicos de codos elevados
    private var lastErrorTime: Long = 0 // Marca de tiempo del último error contado
    private val minErrorInterval: Long = 2500 // Intervalo mínimo entre errores en milisegundos
    private var errorAlreadyCounted: Boolean = false
    private var speedError: Int = 0
    private var balanceError: Int = 0
    private var previousPoints: List<Pair<Float, Float>>? = null



    private val minTimeBetweenPhases: Long = 500
    private val minEccentricTime: Long = 1200 // Tiempo mínimo de fase excéntrica en milisegundos
    private var baseSpeed: Double? = null // Velocidad base de referencia
    private val repSpeeds = mutableListOf<Double>() // Velocidades de cada repetición
    private val calibrationReps = 3 // Número de repeticiones iniciales para calibración
    private var calibrated = false // Estado de calibración


    // Nueva función para verificar si las muñecas están por encima de los hombros en el eje Y
    private fun checkWristAboveShoulder(leftWrist: PointF, leftShoulder: PointF, rightWrist: PointF, rightShoulder: PointF): Boolean {
        return leftWrist.y < leftShoulder.y || rightWrist.y < rightShoulder.y
    }


    fun updateRepCount(person: Person) {

        VisualizationUtils.isIncorrectArmMovement = false
        VisualizationUtils.isIncorrectSpeedArmMovement = false
        VisualizationUtils.isIncorrectBalanceArmMovement = false


        // Puntos clave de ambos brazos
        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val leftElbow = person.keyPoints[BodyPart.LEFT_ELBOW.position].coordinate
        val leftWrist = person.keyPoints[BodyPart.LEFT_WRIST.position].coordinate
        val leftHip = person.keyPoints[BodyPart.LEFT_HIP.position].coordinate
        val leftKnee = person.keyPoints[BodyPart.LEFT_KNEE.position].coordinate

        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate
        val rightElbow = person.keyPoints[BodyPart.RIGHT_ELBOW.position].coordinate
        val rightWrist = person.keyPoints[BodyPart.RIGHT_WRIST.position].coordinate
        val rightHip = person.keyPoints[BodyPart.RIGHT_HIP.position].coordinate
        val rightKnee = person.keyPoints[BodyPart.RIGHT_KNEE.position].coordinate


        // Cálculo de ángulos
        val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)

        val flexionThreshold = 60
        val extensionThreshold = 160

        // Restricciones adicionales
        val leftShoulderHipAngle = calculateShoulderHipAngle(leftShoulder, leftHip, leftElbow)
        val rightShoulderHipAngle = calculateShoulderHipAngle(rightShoulder, rightHip, rightElbow)



        // Crear la lista de puntos clave actuales
        val currentPoints = listOf(
            Pair(leftHip.x, leftHip.y),
            Pair(rightHip.x, rightHip.y),
            Pair(leftKnee.x, leftKnee.y),
            Pair(rightKnee.x, rightKnee.y),
            Pair(leftShoulder.x, leftShoulder.y),
            Pair(rightShoulder.x, rightShoulder.y),
            Pair(rightElbow.x, rightElbow.y),
            Pair(leftElbow.x, leftElbow.y),
            Pair(leftWrist.x, leftWrist.y),
            Pair(rightWrist.x, rightWrist.y)
            )


        // Verificar si los puntos clave anteriores son nulos (primer fotograma)
        if (previousPoints == null) {
            previousPoints = currentPoints
            return // Salir de la función en el primer fotograma
        }





        // Verificar que todos los puntos necesarios sean visibles
        if (!allKeyPointsVisible(person, listOf(
                BodyPart.LEFT_SHOULDER.position, BodyPart.RIGHT_SHOULDER.position,
                BodyPart.LEFT_ELBOW.position, BodyPart.RIGHT_ELBOW.position
            ))) {
            println("No todos los puntos clave están visibles: pausa en el conteo y detección de errores")
            return // Salir de la función si falta algún punto clave
        }




        val currentTime = System.currentTimeMillis()

        // Verificar si ha pasado el intervalo mínimo desde el último error
        val canCountError = currentTime - lastErrorTime > minErrorInterval



        // Verificar si las muñecas están por encima de los hombros
        if (checkWristAboveShoulder(leftWrist, leftShoulder, rightWrist, rightShoulder)) {
            if (canCountError) {
                registrarError("Evita mover los hombros")
                balanceError++
                lastErrorTime = currentTime
                errorAlreadyCounted = false
                repCount++

            }
            VisualizationUtils.isIncorrectBalanceArmMovement = true
            return
        }


        // Transición del movimiento en ambos brazos
        val bothInDownPhase = previousPhaseLeft == "down" && previousPhaseRight == "down"
        val bothInUpPhase = previousPhaseLeft == "up" && previousPhaseRight == "up"

        if (bothInDownPhase && leftAngle < flexionThreshold && rightAngle < flexionThreshold) {
            if (leftShoulderHipAngle <= 30 && rightShoulderHipAngle <= 30) {

                previousPhaseLeft = "up"
                previousPhaseRight = "up"
                errorAlreadyCounted = false  // Resetear error para nueva fase


                val timeDifference = currentTime - lastPhaseChangeTime

                if (lastPhaseChangeTime != 0L && timeDifference < minTimeBetweenPhases) {
                    VisualizationUtils.isIncorrectSpeedArmMovement = true
                    val message = if (rirCount < 2) "¡Vamos, tú puedes!" else "Disminuye la velocidad"
                    listener.onIncorrectMovement(message)
                }

                // Calcular la velocidad de esta repetición
                val speed = 1.0 / timeDifference * 1000
                repSpeeds.add(speed)

                // Calibrar velocidad base usando las primeras repeticiones
                if (repCount < calibrationReps) {
                    if (baseSpeed == null) baseSpeed = speed
                    else baseSpeed = (baseSpeed!! * repCount + speed) / (repCount + 1)

                    if (repCount == calibrationReps - 1) calibrated = true
                }

                // Cálculo del RIR basado en velocidad (solo después de la calibración)
                if (calibrated) {
                    val rir = calculateRIR(speed)
                    rirCount = rir
                    listener.onRIRUpdated(rir)
                }

                lastPhaseChangeTime = currentTime
            } else {
                VisualizationUtils.isIncorrectArmMovement = true

                registrarError("Pega los codos a tu cuerpo.")

                PoseDetectionActivity.GlobalVariables.elbowError = true
                // Contar el error
                if (canCountError) {
                    elbowErrorCount++
                    lastErrorTime = currentTime
                    errorAlreadyCounted = false // Permitir que el error se cuente nuevamente
                    repCount++
                }
            }
        } else if (bothInUpPhase && leftAngle > extensionThreshold && rightAngle > extensionThreshold) {
            if (leftShoulderHipAngle <= 30 && rightShoulderHipAngle <= 30) {

                previousPhaseLeft = "down"
                previousPhaseRight = "down"
                errorAlreadyCounted = false  // Resetear error para nueva fase


                if (PoseDetectionActivity.GlobalVariables.elbowError){
                    elbowErrorCount++
                    PoseDetectionActivity.GlobalVariables.elbowError = false
                }


                val timeDifference = currentTime - lastPhaseChangeTime

                if (lastPhaseChangeTime != 0L && timeDifference < minEccentricTime) {
                    VisualizationUtils.isIncorrectSpeedArmMovement = true
                    val message = if (rirCount < 2) "¡Vamos, tú puedes!" else "Disminuye la velocidad"
                    if (message=="Disminuye la velocidad"){
                        speedError++
                    }
                    listener.onIncorrectMovement(message)
                }


                lastPhaseChangeTime = currentTime
                repCount++
            } else {
                VisualizationUtils.isIncorrectArmMovement = true

                PoseDetectionActivity.GlobalVariables.elbowError = true // Marcar que se ha registrado el error

                registrarError("Pega los codos a tu cuerpo.")
                // Contar el error
                if (canCountError) {
                    elbowErrorCount++
                    lastErrorTime = currentTime
                    errorAlreadyCounted = false // Permitir que el error se cuente nuevamente
                    repCount++
                }
            }
        }
    }

    private fun calculateRIR(currentSpeed: Double): Int {
        val base = baseSpeed ?: return 4 // Si no hay base, asume RIR alto

        val speedRatio = currentSpeed / base
        return when {
            speedRatio >= 1 -> 4
            speedRatio > 0.9 -> 3 // Velocidad cae menos del 10%, RIR 3
            speedRatio > 0.8 -> 2 // Velocidad cae entre 10-20%, RIR 2
            speedRatio > 0.7 -> 1 // Velocidad cae entre 20-30%, RIR 1
            else -> 0          // Velocidad cae más del 30%, RIR 0
        }
    }

    fun calculateAngle(shoulder: PointF, elbow: PointF, wrist: PointF): Double {
        val angle = Math.toDegrees(
            atan2(
                (wrist.y - elbow.y).toDouble(), (wrist.x - elbow.x).toDouble()) -
                    atan2((shoulder.y - elbow.y).toDouble(), (shoulder.x - elbow.x).toDouble())
        )
        return abs(angle)
    }

    fun calculateShoulderHipAngle(shoulder: PointF, hip: PointF, elbow: PointF): Double {
        val shoulderToHip = PointF(hip.x - shoulder.x, hip.y - shoulder.y)
        val shoulderToElbow = PointF(elbow.x - shoulder.x, elbow.y - shoulder.y)

        val dotProduct = (shoulderToHip.x * shoulderToElbow.x + shoulderToHip.y * shoulderToElbow.y)
        val magnitudeHip = sqrt((shoulderToHip.x * shoulderToHip.x + shoulderToHip.y * shoulderToHip.y).toDouble())
        val magnitudeElbow = sqrt((shoulderToElbow.x * shoulderToElbow.x + shoulderToElbow.y * shoulderToElbow.y).toDouble())

        val angle = acos(dotProduct / (magnitudeHip * magnitudeElbow))
        return Math.toDegrees(angle)
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


    // Método auxiliar para verificar que todos los puntos requeridos son visibles
    private fun allKeyPointsVisible(person: Person, requiredPoints: List<Int>): Boolean {
        for (index in requiredPoints) {
            val keyPoint = person.keyPoints[index]
            // Aquí, se verifica la confianza; ajusta el valor 0.5 según lo que consideres como "visible"
            if (keyPoint.score < 0.5) {
                return false // Si algún punto tiene baja confianza, retorna falso
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

    fun getErrorCount(): Int {
        return elbowErrorCount
    }

    fun getSpeedError(): Int {
        return speedError
    }

    fun getBalanceError(): Int {
        return balanceError
    }

    fun getErrorRepCount(): Int {
        return balanceError + speedError + elbowErrorCount
    }

    fun getPerfectRepCount(): Int {
        val perfectRep = repCount - getErrorRepCount()
        return if (perfectRep < 0) 0 else perfectRep
    }


    private fun registrarError(mensaje: String) {
        listener.onIncorrectMovement(mensaje)
    }



    interface BicepCurlCounterListener {
        fun onIncorrectMovement(message: String)
        fun onRIRUpdated(rir: Int)
    }
}
