package com.valdi.perfectrepapp.ml

import android.graphics.Bitmap
import com.valdi.perfectrepapp.data.Person

interface PoseDetector : AutoCloseable {

    fun estimatePoses(bitmap: Bitmap): List<Person>

    fun lastInferenceTimeNanos(): Long
}