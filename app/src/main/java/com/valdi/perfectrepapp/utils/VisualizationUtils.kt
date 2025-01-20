package com.valdi.perfectrepapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.valdi.perfectrepapp.data.BodyPart
import com.valdi.perfectrepapp.data.Person
import kotlin.math.max

object VisualizationUtils {

    var isIncorrectArmMovement: Boolean = false
    var isIncorrectSpeedArmMovement: Boolean = false
    var isIncorrectKneeMovement: Boolean = false
    var isIncorrectSpeedLegMovement: Boolean = false
    var isIncorrectTrunkMovement: Boolean = false
    var isIncorrectShoulderMovement: Boolean = false
    var isIncorrectSpeedShoulderMovement: Boolean = false
    var isIncorrectBalanceShoulderMovement: Boolean = false
    var isIncorrectBalanceArmMovement: Boolean = false


    private const val CIRCLE_RADIUS = 4f
    private const val LINE_WIDTH = 1f
    private const val PERSON_ID_TEXT_SIZE = 30f
    private const val PERSON_ID_MARGIN = 6f

    private val bodyJoints = listOf(
        Pair(BodyPart.NOSE, BodyPart.LEFT_EYE),
        Pair(BodyPart.NOSE, BodyPart.RIGHT_EYE),
        Pair(BodyPart.LEFT_EYE, BodyPart.LEFT_EAR),
        Pair(BodyPart.RIGHT_EYE, BodyPart.RIGHT_EAR),
        Pair(BodyPart.NOSE, BodyPart.LEFT_SHOULDER),
        Pair(BodyPart.NOSE, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
        Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP),
        Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
        Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
        Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
        Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
        Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)
    )

    private val targetJointsArmsError = listOf(
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
        Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST)
    )

    private val targetJointsArmsBalanceError = listOf(
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
    )


    private val targetJointsLegsError = listOf(
        Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
        Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
        Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE),
        Pair(BodyPart.RIGHT_HIP, BodyPart.LEFT_KNEE)
    )

    private val targetJointsTrunkError = listOf(
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP),
        Pair(BodyPart.RIGHT_HIP, BodyPart.LEFT_HIP)
    )

    private val targetJointsShoulderError = listOf(
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW)
    )







    fun drawBodyKeypoints(
        input: Bitmap,
        persons: List<Person>,
        isTrackerEnabled: Boolean = false
    ): Bitmap {
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val originalSizeCanvas = Canvas(output)

        val paintCircleNormal = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.GREEN
            style = Paint.Style.STROKE
        }
        val paintLineNormal = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

        val paintCircleError = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.RED
            style = Paint.Style.STROKE
        }
        val paintCircleSpeedError = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.YELLOW
            style = Paint.Style.STROKE
        }
        val paintLineError = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.RED
            style = Paint.Style.STROKE
        }

        val paintLineSpeedError = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.YELLOW
            style = Paint.Style.STROKE
        }

        val paintLineBalanceError = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.rgb(255, 165, 0)  // Naranja en RGB
            style = Paint.Style.STROKE
        }

        val paintCircleBalanceError = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.RED
            style = Paint.Style.STROKE
        }

        persons.forEach { person ->
            if (isTrackerEnabled) {
                person.boundingBox?.let {
                    originalSizeCanvas.drawText(
                        person.id.toString(),
                        max(0f, it.left),
                        max(0f, it.top) - PERSON_ID_MARGIN,
                        Paint().apply { textSize = PERSON_ID_TEXT_SIZE; color = Color.BLUE }
                    )
                    originalSizeCanvas.drawRect(it, paintLineNormal)
                }
            }

            bodyJoints.forEach { (partA, partB) ->
                val pointA = person.keyPoints[partA.position].coordinate
                val pointB = person.keyPoints[partB.position].coordinate
                val paintLine = when {
                    isIncorrectArmMovement && targetJointsArmsError.contains(Pair(partA, partB)) -> paintLineError
                    isIncorrectKneeMovement && targetJointsLegsError.contains(Pair(partA, partB)) -> paintLineError
                    isIncorrectTrunkMovement && targetJointsTrunkError.contains(Pair(partA, partB)) -> paintLineError
                    isIncorrectShoulderMovement && targetJointsShoulderError .contains(Pair(partA, partB)) -> paintLineError
                    isIncorrectSpeedLegMovement && targetJointsLegsError.contains(Pair(partA, partB)) -> paintLineSpeedError
                    isIncorrectSpeedArmMovement && targetJointsArmsError.contains(Pair(partA, partB)) -> paintLineSpeedError
                    isIncorrectBalanceShoulderMovement && targetJointsShoulderError.contains(Pair(partA, partB)) -> paintLineBalanceError
                    isIncorrectBalanceArmMovement && targetJointsArmsBalanceError.contains(Pair(partA, partB)) -> paintLineError
                    else -> paintLineNormal
                }
                originalSizeCanvas.drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paintLine)
            }

            person.keyPoints.forEach { keyPoint ->
                val paintCircle = when {
                    isIncorrectArmMovement && isArmPart(keyPoint.bodyPart) -> paintCircleError
                    isIncorrectKneeMovement && isLegPart(keyPoint.bodyPart) -> paintCircleError
                    isIncorrectTrunkMovement && isTrunkPart(keyPoint.bodyPart) -> paintCircleError
                    isIncorrectShoulderMovement && isShoulderPart(keyPoint.bodyPart) -> paintCircleError
                    isIncorrectSpeedLegMovement && isLegPart(keyPoint.bodyPart) -> paintCircleSpeedError
                    isIncorrectSpeedArmMovement && isArmPart(keyPoint.bodyPart) -> paintCircleSpeedError
                    isIncorrectBalanceShoulderMovement && isShoulderPart(keyPoint.bodyPart) -> paintCircleBalanceError
                    isIncorrectBalanceArmMovement && isShoulderPart(keyPoint.bodyPart) -> paintCircleError
                    else -> paintCircleNormal
                }
                originalSizeCanvas.drawCircle(keyPoint.coordinate.x, keyPoint.coordinate.y, CIRCLE_RADIUS, paintCircle)
            }
        }

        return output
    }

    private fun isArmPart(bodyPart: BodyPart): Boolean {
        return bodyPart in listOf(
            BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST,
            BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST
        )
    }

    private fun isLegPart(bodyPart: BodyPart): Boolean {
        return bodyPart in listOf(
            BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE,
            BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE
        )
    }

    private fun isTrunkPart(bodyPart: BodyPart): Boolean {
        return bodyPart in listOf(
            BodyPart.LEFT_HIP, BodyPart.LEFT_SHOULDER,
            BodyPart.RIGHT_HIP, BodyPart.RIGHT_SHOULDER
        )
    }

    private fun isShoulderPart(bodyPart: BodyPart): Boolean {
        return bodyPart in listOf(
            BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW,
            BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW
        )
    }



    fun incorrectElbowMovement() {
        isIncorrectArmMovement = true
    }

    fun correctElbowMovement() {
        isIncorrectArmMovement = false
    }

    fun incorrectKneeMovement() {
        isIncorrectKneeMovement = true
    }

    fun correctKneeMovement() {
        isIncorrectKneeMovement = false
    }
}
