package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.Connection
import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.rendering.GameRenderer
import com.roshanah.rt3.recieve
import org.openrndr.Program
import org.openrndr.launch
import org.openrndr.math.Matrix44

class Multiplayer(program: Program, transform: Matrix44, serial: String, val connection: Connection) :
    Scene(program, transform) {
    val renderer = GameRenderer(0.0, 0.0, 1.0, program)
    val player = GamePlayer(serial)
    private var pendingMove = false

    init {
        renderer.game = player.game
        attachInput(renderer) {
            program.launch {
                tryMove(it)
                pendingMove = false
            }
        }

        attachSaving(player) {
            println(it)
        }
    }

    override fun Program.renderBody() {
        renderer.render()
    }

    suspend fun tryMove(move: Int): Boolean {
        if (pendingMove) return false
        pendingMove = true
        val moveDigits = player.game.possibleMoves.toString().length
        val newGame = player.serialize() + move.toString().padStart(moveDigits, '0')
        if (connection.query(newGame) == "valid") {
            player.makeMove(move)
            renderer.game = player.game
            return true
        }
        return false
    }
}

val clientReceive: Multiplayer.(String) -> Unit = {
    recieve(it) {
        prefix("info") {
            println("server: $command")
        }
        prefix("game") {
            val move = command.substring(player.serialize().length).toInt()
            player.makeMove(move)
            renderer.game = player.game
        }
    }
}
