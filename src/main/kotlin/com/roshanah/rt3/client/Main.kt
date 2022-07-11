package com.roshanah.rt3.client

import com.roshanah.rt3.client.rendering.times
//import com.roshanah.recursiveTac.client.scenes.MainMenu
import com.roshanah.rt3.client.scenes.Scene
import com.roshanah.rt3.client.scenes.mainMenu
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import kotlin.math.E
import kotlin.math.pow

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

//        var net = Network.random(2, 1, 100, 1)
////        var net = Network.deserialize("[[[0.33740999424066503, -0.744815912444695, -0.6141708903689936], [-0.7686361632872968, -0.6956511079038626, -0.31830291627848895], [0.5462729066456912, 0.18108963821736188, 0.7712817132301664]], [[0.08626614790625421, 0.30875598674710325, 0.48654591879515413, -0.16166702599224775]]]")
//        println(net)
//        println(net.serialized)
//        val x = listOf(
//            listOf(0.0, 0.0),
//            listOf(1.0, 0.0),
//            listOf(0.0, 1.0),
//            listOf(1.0, 1.0),
//        )
//        val y = listOf(
//            listOf(0.0),
//            listOf(1.0),
//            listOf(1.0),
//            listOf(0.0),
//        )
//
//        println("training took: ${measureNanoTime { for(i in 0 until 5000){
//            var sum = net * 0.0
//            for(d in x.indices)
//                sum += net.computeGradient(x[d], y[d])
//            sum /= x.size.toDouble()
//            net = net.descend(sum, 0.1)
//        } } * 1e-9} seconds")
//
//        println(net)
//
//        for(i in x.indices){
//            println("(${x[i]}, ${y[i]}) -> ${net.fire(x[i]).last()}")
//        }


//        val testGame = Game(0)
//            .getMove(0, Player.X)
//            .getMove(3, Player.O)
//            .getMove(6, Player.X)
//            .getMove(4, Player.O)
//            .getMove(1, Player.X)
//
//        println(testGame.encode(Player.O))


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

//fun Program.renderIcon(){
//    drawer.view *= sizeTransform
//
//    drawer.fill = ColorRGBa.PINK
//    drawer.stroke = null
//    drawer.shapes(ai)
//}



