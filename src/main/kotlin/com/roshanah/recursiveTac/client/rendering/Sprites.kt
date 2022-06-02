package com.roshanah.recursiveTac.client.rendering

import org.openrndr.extra.shapes.RoundedRectangle
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.*

private val headSize = 0.2
private val neckLength = 0.05
private val torsoThickness = 0.5
private val torsoRoundness = 0.1

val player: List<Shape> = listOf(
    Circle(0.5, headSize, headSize).shape,
    RoundedRectangle(
        0.5 - torsoThickness * 0.5,
        headSize * 2 + neckLength, torsoThickness,
        1.0 - headSize * 2 - neckLength,
        torsoRoundness
    ).shape
)


private val offset = 0.25

val multiplayer: List<Shape> = buildList {
    val big = listOf(
        Circle(0.5, headSize, headSize + offset * 0.25).shape,
        RoundedRectangle(
            0.5 - torsoThickness * 0.5 - offset * 0.25,
            headSize * 2 + neckLength - offset * 0.25,
            torsoThickness + offset * 0.5,
            1.0 - headSize * 2 - neckLength + offset * 0.5,
            torsoRoundness + offset * 0.25
        ).shape
    )
    val translated = player.map {
        it.transform(transform {
            translate(offset, -offset)
        })
    }

    addAll(player)
    (0..1).map {
        addAll(compound {
            difference {
                shape(translated[it])
                shape(big[it])
                shape(big[1 - it])
            }
        })
    }
}.map {
    it.transform(transform {
        scale(1.0 / (1.0 + offset))
        translate(0.0, offset)
    })
}

private val nodeRad = 0.125
private val connectionThickness = 0.01

val ai = buildList {
    val spread = 1.0 - nodeRad * 2
    val inputs = mutableListOf<Vector2>()
    val outputs = mutableListOf<Vector2>()


    for(i in 0 until 3){
        val center = Vector2(nodeRad, nodeRad + spread / 2 * i)
        inputs.add(center)
        add(Circle(center, nodeRad).shape)
    }

    for(i in 0 until 2){
        val center = Vector2(1.0 - nodeRad, nodeRad + spread / 4 + spread / 2 * i)
        outputs.add(center)
        add(Circle(center, nodeRad).shape)
    }

    for(input in inputs){
        for(output in outputs){
            val diff = output - input
            val normal = diff.perpendicular().normalized * connectionThickness
            add(contour {
                moveTo(input + normal)
                lineTo(output + normal)
                lineTo(output - normal)
                lineTo(input - normal)
                lineTo(anchor)
                close()
            }.shape)
        }
    }
}


private val RoundedRectangle.shape: Shape
    get() = Shape(listOf(this.contour))

val List<Shape>.bounds: Rectangle
    get() = this.map{it.bounds}.bounds