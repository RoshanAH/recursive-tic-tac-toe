package com.roshanah.recursiveTac.client.rendering

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2


data class Slot(val stack: String, val transform: Matrix44, val type: SlotType, var development: Double){
    var developing: Boolean = true
    companion object {
        var rate = 5.0
        var thickness = 0.15
    }
}

enum class SlotType {
    BOARD, X, O;
}