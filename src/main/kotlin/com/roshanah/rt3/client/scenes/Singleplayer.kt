package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.rendering.GameRenderer
import org.openrndr.Program
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44

class Singleplayer(program: Program, transform: Matrix44, depth: Int) : Scene(program, transform){
    val renderer = GameRenderer(0.0, 0.0, 1.0, program)
    val player = GamePlayer(depth)
    init {
        renderer.game = player.game
        attachInput(renderer){
            player.makeMove(it)
            renderer.game = player.game
        }
        attachSaving(player) {
            println(it)
        }
    }

    override fun Program.renderBody() = drawer.isolated {
        renderer.render()
    }


}