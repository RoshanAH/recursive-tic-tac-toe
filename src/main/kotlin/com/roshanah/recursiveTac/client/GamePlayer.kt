package com.roshanah.recursiveTac.client

import com.roshanah.recursiveTac.client.elements.Game
import com.roshanah.recursiveTac.client.elements.Player
import java.util.*


class GamePlayer {
    val history: MutableList<Game>
    val moveHistory: MutableList<Int>
    val player: Player get() = if (currentIndex % 2 == 0) Player.X else Player.O
    var currentIndex = 0
        set(value) {
            field = value.coerceIn(0 until history.size)
        }
    val depth: Int

    constructor(depth: Int) {
        this.depth = depth
        history = mutableListOf()
        moveHistory = mutableListOf()
        history.add(Game(depth))
    }

    private constructor(depth: Int, history: List<Game>, moveHistory: List<Int>){
        this.depth = depth
        this.history = history.toMutableList()
        this.moveHistory = moveHistory.toMutableList()
        currentIndex = history.size - 1
    }

    fun fork() = GamePlayer(depth, history.subList(0, currentIndex + 1), moveHistory.subList(0, currentIndex))


    val game: Game
        get() = history[currentIndex]

    fun undo() {
        if (currentIndex > 0) {
            currentIndex--
        }
    }

    fun redo() {
        if (currentIndex < history.size - 1) {
            currentIndex++
        }
    }

    constructor(serial: String) {
        history = mutableListOf()
        moveHistory = mutableListOf()
        val split = serial.split("-")
        require(!(split.size > 2 || split.isEmpty())) { "invalid serial" }
        if (split.size == 1) {
            try {
                depth = split[0].toInt()
                history.add(Game(depth))
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("invalid serial")
            }
        } else {
            try {
                depth = split[0].toInt()
                history.add(Game(depth))
                val history = split[1]
                var i = 0
                while (i < history.length) {
                    val length = getMoveLength(this.history.last())
                    makeMove(history.substring(i, i + length).toInt())
                    i += length
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("invalid serial")
            }
        }
    }

    fun makeMove(move: Int) {
        currentIndex = history.size - 1
        history.add(game.getMove(move, player))
        moveHistory.add(move)
        currentIndex++
    }

    fun returnToCurrent() {
        for (i in currentIndex until history.size - 1) redo()
    }

    fun serialize(): String {
        var out = "$depth-"
        for (i in moveHistory.indices) {
            val length = getMoveLength(history[i])
            out += String.format("%0" + length + "d", moveHistory[i])
        }
        return out
    }
}

private fun getMoveLength(g: Game): Int {
    return (g.possibleMoves.toString()).length
}