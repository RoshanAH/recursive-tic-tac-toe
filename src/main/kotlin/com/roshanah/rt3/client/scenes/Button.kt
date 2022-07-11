package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.scenes
import com.roshanah.rt3.client.zoom
import org.openrndr.math.Matrix44
import org.openrndr.shape.Shape

class Button(val transform: Matrix44,
             val sprite: List<Shape>,
             var opacity: Double,
             var hoverEffect: Double,
             val scene: Scene) {
    var targetOpacity = 1.0

    var onClick: () -> Unit = {
        zoom(transform)
        scenes.add(scene)
        targetOpacity = 0.0
    }

    companion object {
        const val hoverDisplacement = 0.2
    }
}