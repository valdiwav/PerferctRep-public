package com.valdi.perfectrepapp.utils.counters

import com.valdi.perfectrepapp.data.BodyPart
import com.valdi.perfectrepapp.data.Person
import com.valdi.perfectrepapp.utils.VisualizationUtils
import kotlin.math.*

class SquatCounter(private val listener: SquatCounterListener)  {

    private var previousPhase: String = "up"
    private var repCount: Int = 0
    private var speedError: Int = 0
    private var lastPhaseChangeTime: Long = 0

    private var forceImbalanceErrorCount: Int = 0
    private var kneeErrorCount: Int = 0
    private val minErrorInterval: Long = 2000 // Intervalo mínimo entre errores en milisegundos
    private var errorAlreadyCounted: Boolean = false
    private var lastErrorTime: Long = 0 // Marca de tiempo del último error contado


    // Variables para el cálculo de RIR
    private val repSpeeds = mutableListOf<Double>()
    private val calibrationReps = 3
    private var baseSpeed: Double? = null
    private var calibrated = false
    private var rirCount: Int = 4

    private var previousPoints: List<Pair<Float, Float>>? = null

    // Variables para la detección de la posición inicial
    private var initialized: Boolean = false
    private val minKneeAngleForInitialization = 160.0  // Ángulo mínimo para considerar que las piernas están extendidas

    private var eccentricStartTime: Long = 0
    private val minEccentricDuration = 1000 //milisegundos

    // Umbral de diferencia de altura entre caderas y hombros
    private val hipHeightThreshold = 10.0 // Ajusta según el nivel de sensibilidad deseado
    private val shoulderHeightThreshold = 10.0 // Ajusta según el nivel de sensibilidad deseado

    // Método para detectar desbalance en la distribución de fuerza
    private fun detectForceImbalance(person: Person): Boolean {
        val leftHip = person.keyPoints[BodyPart.LEFT_HIP.position].coordinate
        val rightHip = person.keyPoints[BodyPart.RIGHT_HIP.position].coordinate
        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate

        // Calcular diferencia en altura entre caderas y entre hombros
        val hipHeightDifference = abs(leftHip.y - rightHip.y)
        val shoulderHeightDifference = abs(leftShoulder.y - rightShoulder.y)

        // Verificar si la diferencia en altura excede el umbral
        if (hipHeightDifference > hipHeightThreshold) {
            println("Desbalance detectado en las caderas: posible sobrecarga en una pierna.")
            VisualizationUtils.isIncorrectKneeMovement = true
            listener.onIncorrectMovement("Mantén el equilibrio")
            return true
        }

        if (shoulderHeightDifference > shoulderHeightThreshold) {
            println("Desbalance detectado en los hombros: posible inclinación del torso.")
            VisualizationUtils.isIncorrectTrunkMovement = true
            listener.onIncorrectMovement("Mantén el torso recto")
            return true
        }

        return false
    }



    // Método para calcular el ángulo entre tres puntos
    private fun calculateAngle(p1: Pair<Float, Float>, p2: Pair<Float, Float>, p3: Pair<Float, Float>): Double {
        val angle = Math.toDegrees(
            atan2((p3.second - p2.second).toDouble(), (p3.first - p2.first).toDouble()) -
                    atan2((p1.second - p2.second).toDouble(), (p1.first - p2.first).toDouble())
        )
        return abs(angle).let { if (it > 180) 360 - it else it }
    }

    // Nueva función para detectar el valgo de rodilla
    private fun detectKneeValgus(leftHipX: Float, leftKneeX: Float): Boolean {
        val valgusThreshold = 10.0 // Ajusta este umbral según sea necesario
        return leftKneeX < leftHipX - valgusThreshold
    }

    fun updateRepCount(person: Person) {

        VisualizationUtils.isIncorrectKneeMovement = false
        VisualizationUtils.isIncorrectTrunkMovement = false

        val leftHip = person.keyPoints[BodyPart.LEFT_HIP.position].coordinate
        val rightHip = person.keyPoints[BodyPart.RIGHT_HIP.position].coordinate
        val leftKnee = person.keyPoints[BodyPart.LEFT_KNEE.position].coordinate
        val rightKnee = person.keyPoints[BodyPart.RIGHT_KNEE.position].coordinate
        val leftAnkle = person.keyPoints[BodyPart.LEFT_ANKLE.position].coordinate
        val rightAnkle = person.keyPoints[BodyPart.RIGHT_ANKLE.position].coordinate
        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate

        // Verificar que todos los puntos necesarios sean visibles
        if (!allKeyPointsVisible(person, listOf(
                BodyPart.LEFT_HIP.position, BodyPart.RIGHT_HIP.position,
                BodyPart.LEFT_KNEE.position, BodyPart.RIGHT_KNEE.position,
                BodyPart.LEFT_ANKLE.position, BodyPart.RIGHT_ANKLE.position
            ))) {
            println("No todos los puntos clave están visibles: pausa en el conteo y detección de errores")
            return // Salir de la función si falta algún punto clave
        }

        val currentTime = System.currentTimeMillis()


        if (detectForceImbalance(person) && canCountError(currentTime)) {
            forceImbalanceErrorCount++
            lastErrorTime = currentTime // Actualizamos el último tiempo del error
            listener.onIncorrectMovement("Mantén el equilibrio")
        }


        // Definir puntos para el cálculo de ángulo de la rodilla
        val hipPoint = Pair(leftHip.x, leftHip.y)
        val kneePoint = Pair(leftKnee.x, leftKnee.y)
        val anklePoint = Pair(leftAnkle.x, leftAnkle.y)

        // Calcular el ángulo de la rodilla
        val kneeAngle = calculateAngle(hipPoint, kneePoint, anklePoint)

        // Verificar si el usuario está en la posición inicial (piernas extendidas)
        if (!initialized) {
            if (kneeAngle > minKneeAngleForInitialization) {
                initialized = true
                println("Posición inicial detectada: ángulo de rodilla = $kneeAngle")
            } else {
                println("Esperando que el usuario esté en posición inicial...")
                return  // Salir si no se ha detectado la posición inicial
            }
        }

        // Definir los umbrales de ángulo para las fases
        val partialSquatThreshold = 120.0    // Umbral para detectar inicio de subida
        val fullStandThreshold = 170.0       // Umbral para confirmar que estás de pie

        // Crear la lista de puntos clave actuales
        val currentPoints = listOf(
            Pair(leftHip.x, leftHip.y),
            Pair(rightHip.x, rightHip.y),
            Pair(leftKnee.x, leftKnee.y),
            Pair(rightKnee.x, rightKnee.y),
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

        // Actualizar los puntos clave anteriores con los puntos actuales
        previousPoints = currentPoints


        // Llamar a la función para detectar valgo de rodilla
        if (detectKneeValgus(leftHip.x, leftKnee.x) && canCountError(currentTime)) {
            kneeErrorCount++
            lastErrorTime = currentTime // Actualizamos el último tiempo del error
            listener.onIncorrectMovement("No juntes las rodillas")
        }


        if (previousPhase == "up") {
            // Comienza la fase de descenso cuando el ángulo es menor que el umbral de sentadilla parcial
            if (kneeAngle < partialSquatThreshold) {
                previousPhase = "down"
                eccentricStartTime = currentTime
                println("Fase de descenso: ángulo de rodilla = $kneeAngle")
                lastPhaseChangeTime = currentTime

            }
        } else if (previousPhase == "down") {
            // Confirma que has subido completamente y estás de pie antes de contar la repetición
            if (kneeAngle > fullStandThreshold) {
                // Si el ángulo indica que estás de pie (completamente extendido)
                previousPhase = "up"
                repCount++
                println("Repetición completada: $repCount, ángulo de rodilla = $kneeAngle")

                val eccentricDuration = currentTime - eccentricStartTime
                if (eccentricDuration < minEccentricDuration){
                    listener.onIncorrectMovement("Controla la bajada")
                    speedError++
                }

                // Calcular la velocidad de subida
                val timeDifference = currentTime - lastPhaseChangeTime
                val speed = 1.0 / timeDifference * 1000  // Velocidad en repeticiones por segundo
                repSpeeds.add(speed)

                // Calibración de la velocidad base usando las primeras repeticiones
                if (repCount <= calibrationReps) {
                    baseSpeed = (baseSpeed?.times(repCount - 1) ?: (0.0 + speed)) / repCount
                    if (repCount == calibrationReps) calibrated = true
                }

                // Calcular el RIR basado en la velocidad después de la calibración
                if (calibrated) {
                    rirCount = calculateRIR(speed)
                    listener.onRIRUpdated(rirCount)
                }

                lastPhaseChangeTime = currentTime
            }
        }

    }


    // Método mejorado para calcular el RIR en función de la velocidad actual y las últimas velocidades
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



    // Método para obtener el conteo de repeticiones
    fun getRepCount(): Int {
        return repCount
    }

    // Método para obtener el último RIR calculado
    fun getLastRIR(): Int {
        return rirCount
    }

    fun getSpeedError(): Int {
        return speedError
    }

    fun getForceImbalanceErrorCount(): Int {
        return forceImbalanceErrorCount
    }

    fun getKneeErrorCount(): Int {
        return kneeErrorCount
    }

    private fun canCountError(currentTime: Long): Boolean {
        return currentTime - lastErrorTime > minErrorInterval
    }


    fun getErrorRepCount(): Int {
        return forceImbalanceErrorCount + speedError + kneeErrorCount
    }

    fun getPerfectRepCount(): Int {
        val perfectRep = repCount - getErrorRepCount()
        return if (perfectRep < 0) 0 else perfectRep
    }



    // Interfaz para la actualización de RIR
    interface SquatCounterListener {
        fun onRIRUpdated(rir: Int)
        fun onIncorrectMovement(message: String)
    }

}
