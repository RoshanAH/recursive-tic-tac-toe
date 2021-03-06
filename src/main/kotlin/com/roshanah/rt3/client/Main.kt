package com.roshanah.rt3.client

import com.roshanah.rt3.client.elements.Game
import com.roshanah.rt3.client.elements.Player
import com.roshanah.rt3.client.rendering.create
import com.roshanah.rt3.client.rendering.join
import com.roshanah.rt3.client.rendering.times
//import com.roshanah.recursiveTac.client.scenes.MainMenu
import com.roshanah.rt3.client.scenes.Scene
import com.roshanah.rt3.client.scenes.mainMenu
import com.roshanah.rt3.client.training.diagnol1Mirror
import com.roshanah.rt3.client.training.diagnol2Mirror
import com.roshanah.rt3.client.training.invoke
import com.roshanah.rt3.client.training.normal
import kotlinx.coroutines.delay
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import kotlin.math.E
import kotlin.math.pow

val SERVER_IP = ""

var globalCameraPos = Vector2(0.5, 0.5)
    private set(value) {
        field = value
        transformRecalculate = true
    }
var globalZoom = 1.0
    private set(value) {
        field = value
        transformRecalculate = true
    }

private var frames = mutableListOf<Double>()

var targetZoom: Double = globalZoom
var targetCameraPos: Vector2 = globalCameraPos

var renderedObjects: Int = 0

val foreground: ColorRGBa = ColorRGBa.PINK
val background: ColorRGBa = ColorRGBa.BLACK

fun zoom(transform: Matrix44) {
    val inverse = transform.inversed
    val zero = inverse * Vector2.ZERO
    val one = inverse * Vector2.ONE
    val diff = one - zero
    val minSize = diff.x.coerceAtMost(diff.y)

    targetCameraPos = transform * Vector2(0.5, 0.5)
    targetZoom = minSize
}

private var transformRecalculate = true

lateinit var cameraTransform: Matrix44
    private set
lateinit var sizeTransform: Matrix44
    private set

var scenes = mutableListOf<Scene>()
lateinit var font: FontImageMap


fun main() = application {
    configure {
        windowResizable = true
        width = 600
        height = 600
        title = "Recursive Tic Tac Toe"
        vsync = false

    }


    program {

        font = loadFont("data/fonts/jetbrains-mono.ttf", 300.0)

        sizeTransform = transform {
            val minLen = width.coerceAtMost(height).toDouble()
            translate((width - minLen) * 0.5, (height - minLen) * 0.5)
            scale(minLen)
        }

        cameraTransform = transform {
            translate(Vector2.ONE * 0.5 - globalCameraPos * globalZoom)
            scale(globalZoom)
        }

        scenes.add(mainMenu(this))

        var target = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        window.sized.listen {
            sizeTransform = transform {
                val minLen = width.coerceAtMost(height).toDouble()
                translate((width - minLen) * 0.5, (height - minLen) * 0.5)
                scale(minLen)
            }

            target = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }
        }


        extend {
            if (transformRecalculate) {
                cameraTransform = transform {
                    translate(Vector2.ONE * 0.5 - globalCameraPos * globalZoom)
                    scale(globalZoom)
                }
                transformRecalculate = false
            }

            renderedObjects = 0

            drawer.isolatedWithTarget(target) {
                clear(background)
                fontMap = font
//                testRender()
                render()
//                renderIcon()
            }
            drawer.image(target.colorBuffer(0))

//            println("Rendered Objects: $renderedObjects")
        }
    }
}

fun Program.render() {

    globalCameraPos += (targetCameraPos - globalCameraPos) * (1 - E.pow(-3.0 * deltaTime))
    globalZoom += (targetZoom - globalZoom) * (1 - E.pow(-3.0 * deltaTime))
    drawer.view *= sizeTransform * cameraTransform

    frames.add(seconds)
    frames.removeAll { seconds - it > 1 }

//    println("FPS: ${frames.size}")

    scenes.forEach { it.render() }

}


fun Program.testRender() {

}

fun Program.renderIcon(){
    drawer.view *= sizeTransform

    drawer.fill = ColorRGBa.PINK
    drawer.stroke = null
    drawer.shapes(join)
}



