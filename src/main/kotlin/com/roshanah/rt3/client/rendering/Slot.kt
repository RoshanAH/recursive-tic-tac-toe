package com.roshanah.rt3.client.rendering

import org.openrndr.math.Matrix44


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