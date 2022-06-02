package com.roshanah.recursiveTac.client.scenes

import com.roshanah.recursiveTac.client.scenes
import com.roshanah.recursiveTac.client.zoom
import org.openrndr.math.Matrix44
import org.openrndr.shape.Rectangle
import org.openrndr.shape.TextNode

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