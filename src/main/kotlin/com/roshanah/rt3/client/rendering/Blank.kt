package com.roshanah.rt3.client.rendering

import org.openrndr.math.Matrix44

data class Blank(val transform: Matrix44, var opacity: Double){
    var targetOpacity: Double = 0.0

    companion object{
        var passiveOpacity: Double = 0.3
        var hoverOpacity: Double = 0.6
    }
}