package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.scenes
import com.roshanah.rt3.client.zoom
import org.openrndr.math.Matrix44
import org.openrndr.shape.Rectangle

class TextField(val transform: Matrix44, val width: Double, val scene: (String) -> Scene) {
    val rect = Rectangle(-width * 0.5 + 0.5, 0.0, width, 1.0)
    var focused = false
    var opacity = 1.0
    var targetOpacity = 1.0
    var text: String = ""

    fun onEnter(){
        zoom(transform)
        scenes.add(scene(text))
        targetOpacity = 0.0
    }
}