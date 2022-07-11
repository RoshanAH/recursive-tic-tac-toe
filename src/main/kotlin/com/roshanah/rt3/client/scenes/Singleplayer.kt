package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.cameraTransform
import com.roshanah.rt3.client.rendering.GameRenderer
import com.roshanah.rt3.client.rendering.times
import com.roshanah.rt3.client.sizeTransform
import org.openrndr.KEY_SPACEBAR
import org.openrndr.MouseButton
import org.openrndr.Program
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2

class Singleplayer(program: Program, transform: Matrix44, depth: Int) : Scene(program, transform){
    val renderer = GameRenderer(0.0, 0.0, 1.0, program)
    val player = GamePlayer(depth)
    init {
        renderer.game = player.game
        program.apply{
            mouse.moved.listen {
                renderer.mouse = canvasToBoard(it.position)
            }

            mouse.buttonDown.listen {
                when(it.button){
                    MouseButton.LEFT -> {
                        val move: Int = renderer.hoveredMove ?: return@listen
                        player.makeMove(move)
                        renderer.game = player.game
                    }
                }
            }

            keyboard.keyDown.listen {
                when(it.key){
                    KEY_SPACEBAR -> renderer.showMoves = true
                }
            }

            keyboard.keyUp.listen {
                when(it.key){
                    KEY_SPACEBAR -> renderer.showMoves = false
                }
            }
        }
    }

    override fun Program.renderBody() = drawer.isolated {
        renderer.render()
    }

    private fun canvasToBoard(pos: Vector2) = (sizeTransform * cameraTransform * transform * renderer.fromBoard).inversed * pos

}