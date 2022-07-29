package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.elements.Game
import com.roshanah.rt3.client.rendering.GameRenderer
import com.roshanah.rt3.client.scenes
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.Program
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44

class DepthSelection(program: Program, transform: Matrix44, sceneBuilder: Scene.(Int, Int) -> Scene) : Scene(program, transform) {
    val renderer = GameRenderer(0.0, 0.0, 1.0, program)
    var depth = 0
        private set(value) {
            field = value.coerceIn(0..3)
            renderer.game = Game(field)
            println(depth)
        }
    init {
        renderer.game = Game(0)
        program.apply {
            keyboard.keyDown.listen {
                if(active) {
                    when (it.key) {
                        KEY_ARROW_LEFT -> depth--
                        KEY_ARROW_RIGHT -> depth++
                    }
                }
            }
        }
        attachInput(renderer) {
            scenes.add(sceneBuilder(depth, it))
            scenes.remove(this)
        }
    }

    override fun Program.renderBody() = drawer.isolated {
        renderer.render()
    }
}