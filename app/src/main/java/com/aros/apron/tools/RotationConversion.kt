package com.aros.apron.tools

import org.opencv.core.Mat
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationConversion {

    /**
     * 旋转矩阵 --> 欧拉角
     */
     fun rotationMatrixToEulerAngles(R: Mat): MutableList<Double> {
        val sy = sqrt(R[0, 0][0] * R[0, 0][0] + R[1, 0][0] * R[1, 0][0])
        val singular = sy < 1e-6

        return if (!singular) {
            val x = atan2(R[2, 1][0], R[2, 2][0])
            val y = atan2(-R[2, 0][0], sy)
            val z = atan2(R[1, 0][0], R[0, 0][0])
            mutableListOf(x, y, z)
        } else {
            val x = atan2(-R[1, 2][0], R[1, 1][0])
            val y = atan2(-R[2, 0][0], sy)
            val z = 0.0
            mutableListOf(x, y, z)
        }
    }
}