package com.roshanah.recursiveTac.client.training

import com.roshanah.recursiveTac.client.GamePlayer
import com.roshanah.recursiveTac.client.elements.Game
import com.roshanah.recursiveTac.client.elements.Player
import com.roshanah.recursiveTac.client.elements.Symbol

private typealias Transform = (List<Int>) -> List<Int>

class AmbiguousPlayer(original: GamePlayer) {
    data class Transformed(val player: GamePlayer, val transform: Transform, val inverse: Transform)

    val transforms: List<Transformed> = listOf(
        Transformed(GamePlayer(original.depth), clockwise, counterClockwise),
        Transformed(GamePlayer(original.depth), counterClockwise, clockwise),
        Transformed(GamePlayer(original.depth), rotateTwice, rotateTwice),
        Transformed(GamePlayer(original.depth), verticalMirror, verticalMirror),
        Transformed(GamePlayer(original.depth), horizontalMirror, horizontalMirror),
        Transformed(GamePlayer(original.depth), diagnol1Mirror, diagnol1Mirror),
        Transformed(GamePlayer(original.depth), diagnol2Mirror, diagnol2Mirror),
    )

    val original = Transformed(original, identity, identity)

    var normal: Transformed = this.original
    val normalized get() = normal.player.game

    val history: MutableList<Game> = mutableListOf(original.history.first())
    val moveHistory: MutableList<List<Int>> = mutableListOf()

    fun extractMoves(player: Player): List<Pair<Game, List<Int>>> = buildList {
        for (i in moveHistory.indices) {
            if ((i % 2 == 0) == (player == Player.X))
                add(Pair(history[i], moveHistory[i]))
        }
    }



    init {

        original.moveHistory.forEachIndexed { i, move ->
            val game = original.history[i]
            var max = this.original
            transforms.forEach {
                it.player.makeMove(it.transform(game.positionOf(move)))
                if (it.player.game > max.player.game)
                    max = it
            }
            normal = max
            history += max.player.game
            moveHistory += max.transform(game.positionOf(move))

        }
    }

    private fun updateNormal() {
        var max = this.original
        transforms.forEach {
            if (it.player.game > max.player.game)
                max = it
        }
        normal = max
    }

//    fun makeMove(move: Int) {
////        println("move position: ${original.game.positionOf(move)}")
//
//        transforms.forEach {
//            it.player.makeMove(it.transform(original.player.game.positionOf(move)))
//        }
//
//        updateNormal()
//
//        original.player.makeMove(move)
//    }
//
//    fun makeMove(slot: List<Int>) = makeMove(original.player.game.positionToMove(slot))

    fun makeMove(move: Int) = makeMove(normalized.positionOf(move))
    fun makeMove(slot: List<Int>) {
        val originalMove = normal.inverse(slot)

        original.player.makeMove(originalMove)
        transforms.forEach {
            it.player.makeMove(it.transform(originalMove))
        }

        updateNormal()

        history += normalized
        moveHistory += slot
    }

    private fun GamePlayer.makeMove(slot: List<Int>) = makeMove(game.positionToMove(slot))

    private operator fun Game.compareTo(other: Game): Int {

        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val thisSlot = this[r][c]
                val otherSlot = other[r][c]

                if (thisSlot == null && otherSlot == null)
                    continue
                if (thisSlot is Symbol && otherSlot is Symbol) {
                    if (thisSlot.state == otherSlot.state)
                        continue

                    val value: Symbol.() -> Int = {
                        when (this.state) {
                            Symbol.State.BLANK -> 0
                            Symbol.State.O -> 1
                            Symbol.State.X -> 2
                        }
                    }

                    return thisSlot.value().compareTo(otherSlot.value())
                }

                if (thisSlot is Symbol)
                    return 1

                if (thisSlot !is Symbol && otherSlot is Symbol)
                    return -1

                if (thisSlot is Game && otherSlot is Game) {
                    val compare = thisSlot.compareTo(otherSlot)
                    if (compare != 0) return compare
                }
            }
        }

        return 0
    }

}

val identity: Transform = { it }

val clockwise: Transform = {
    it.map { slot ->
        3 * (slot % 3) - slot / 3 + 2
    }
}

val counterClockwise: Transform = {
    it.map { slot ->
        6 - 3 * (slot % 3) + slot / 3
    }
}

val rotateTwice: Transform = {
    it.map { slot ->
        8 - 3 * (slot / 3) - slot % 3
    }
}

val verticalMirror: Transform = {
    it.map { slot ->
        3 * (slot / 3) - slot % 3 + 2
    }
}

val horizontalMirror: Transform = {
    it.map { slot ->
        6 - 3 * (slot / 3) + slot % 3
    }
}

val diagnol1Mirror: Transform = {
    it.map { slot ->
        3 * (slot % 3) + slot / 3
    }
}

val diagnol2Mirror: Transform = {
    it.map { slot ->
        8 - 3 * (slot % 3) - slot / 3
    }
}

