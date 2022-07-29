package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.elements.*
import com.roshanah.rt3.ml.List1d

import com.roshanah.rt3.ml.Network
import kotlin.math.pow


class Trainer(val depth: Int, network: Network) {

    var player = GamePlayer(depth)
        private set

    var teachers = mutableListOf(network)
    var teacherIndex = 0
        private set(value) {
            field = value % teachers.size
        }
    val teacher: Network get() = teachers[teacherIndex]
    var student: Network = network.random
    var studentPlayer = Player.X
        private set

    var state = State.PLAY
        private set

    var totalGamesPlayed = 0
        private set
    var cost = 0.0
        private set

    var learningRate = 0.2
    val moveSet = mutableSetOf<Pair<List<Double>, List<Double?>>>()


    private val correctionStack = mutableListOf<CorrectionSet>()


    //    A list of network outputs, for each player
    val lastGames = GameMap()
    var minWinRatio = 0.75

    val winRatio: Double
        get() {
            var totalScore = 0.0
            var games = 0
            for (network in lastGames.values) {
                for (game in network.values.filterNotNull()) {
                    games++
                    totalScore += game.score
                }
            }

            return totalScore / games
        }

    val currentGame = mutableListOf<Move>()

    fun update() {
//        println("${currentGame.size}, ${correctionStack.size}, $state")
        when (state) {
            State.PLAY -> {
                val evaluation = player.game.evaluate()
                if (!evaluation.finished) {
                    if (player.player == studentPlayer) {
                        val move = student.calculateMove(player)
                        currentGame += move
                        player.makeMove(move.slot)
                    } else
                        player.makeMove(teacher.calculateMove(player).slot)
                } else {

                    val initial: Boolean = correctionStack.isEmpty()
                    if (initial) {
                        val score = when (evaluation.player) {
                            studentPlayer -> 1.0
                            !studentPlayer -> 0.0
                            else -> 0.5
                        }
                        val last = lastGames[teacher][studentPlayer]
                        val repeated: Boolean =
                            last != null && last.history.containsAll(currentGame) && currentGame.containsAll(last.history)

                        lastGames[teacher][studentPlayer] =
                            GameMap.TeacherMap.LastGame(currentGame.map { it.copy() }, score, repeated)


                        if (repeated) {
                            reset()
                        } else {
                            state = if (evaluation.player == studentPlayer)
                                State.WIN
                            else
                                State.CORRECTION
                        }

                    } else {
                        state = if (evaluation.player == studentPlayer)
                            State.WIN
                        else
                            State.CORRECTION
                    }
                }
            }
            State.WIN -> {
                moveSet.addAll(currentGame.map { Pair(it.encoded, it.normalizedGame.encodeKey(it.normalSlot)) })
                println(moveSet.any { element -> moveSet.filter { it != element }.any { it.first == element.first }})
                reset()
            }
            State.CORRECTION -> {
                for (i in correctionStack.size until currentGame.size) {
                    correctionStack.add(
                        CorrectionSet(
                            currentGame[i].originalGame,
                            currentGame[i].encoded,
                            mutableSetOf(currentGame[i].slot)
                        )
                    )
                }

                while (correctionStack.last().moveSet.size >= correctionStack.last().game.possibleMoves) {
                    correctionStack.removeLast()
                    if (correctionStack.isEmpty()) {
                        reset()
                        return
                    }
                }

//                  Undo the game so the student gets another chance to make a move
                player.currentIndex = (correctionStack.lastIndex) * 2 + if (studentPlayer == Player.X) 0 else 1
                player = player.fork()
                while (currentGame.size > correctionStack.size - 1) currentGame.removeLast()
//                println(moveSet.size)
//                println(lastGames.values.any { it?.repeat == true })

//                val t1 = currentGame.size
//                val t2 = (if(studentPlayer == Player.X) player.moveHistory.filterIndexed {i, _ -> i % 2 == 0}
//                else player.moveHistory.filterIndexed {i, _ -> i % 2 == 1}).size
//                println("$t1, $t2")

                val last = correctionStack.last()
                val moveChoices = buildList {
                    student.fire(last.inputs).last().forEachIndexed { i, value ->
                        val stack = i.slotIndex(depth)
                        if (last.game[stack].active)
                            add(Pair(stack, value))
                    }
                }.sortedByDescending { it.second }

                for (move in moveChoices) {
                    if (!last.moveSet.contains(move.first)) {
                        currentGame += moveFromOriginal(last.game, studentPlayer, move.first)
                        player.makeMove(move.first)
                        last.moveSet.add(move.first)
                        state = State.PLAY
                        break
                    }
                }

            }
            State.DESCENT -> {
                var avg = student.zero
                var totalCost = 0.0
                moveSet.forEach {
                    val fired = student.fire(it.first)
//                    println(fired.last())
//                    println(it.second + "\n")
                    totalCost += fired.last().zip(it.second).sumOf {
                        val key = it.second
                        if (key != null) {
                            (it.first - key).pow(2)
                        } else 0.0
                    }
                    avg += student.computeGradient(fired, it.second)
                }
                avg /= moveSet.size
                cost = totalCost / moveSet.size

//                println(avg.formattedString())

                state = if (winRatio >= minWinRatio) {
                    State.REPLACEMENT
                } else {
                    State.PLAY
                }

                student = student.descend(avg, learningRate)
            }
            State.REPLACEMENT -> {
                teachers += student
                teachers.removeAt(0)
                student = student.random
                state = State.PLAY
                lastGames.map.clear()
                moveSet.clear()
            }
        }
    }

    private fun reset() {
        player = GamePlayer(depth)
        correctionStack.clear()
        currentGame.clear()
        totalGamesPlayed++
        val complete = lastGames[teacher].complete
        if (complete) teacherIndex++
        state = if (teacherIndex == 0 && complete) State.DESCENT
        else State.PLAY
        studentPlayer = !studentPlayer
    }

    constructor(depth: Int, layers: Int, nodes: Int) : this(
        depth,
        Network.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, layers, nodes, 9.0.pow(depth + 1).toInt())
    )


    enum class State {
        PLAY, CORRECTION, WIN, DESCENT, REPLACEMENT
    }

}

fun Game.encodeKey(slot: SlotIndex): List<Double?> {
    val out = mutableListOf<Double?>()

    foreachIndexed { i, it ->
        if (i.stack.size != depth + 1) return@foreachIndexed // iterate only on symbols
        out += when {
            i == slot -> 1.0
            it.active -> 0.0
            else -> null
        }
    }

    return out
}


fun Network.calculateMove(player: GamePlayer): Move {
    val (normal, transform) = player.game.normal
    val inputs = buildList {
        normal.foreach {
            add(
                if (it is Symbol && (it.state == player.player.symbolState)) // self
                    1.0
                else
                    0.0
            )

            add(
                if (it is Symbol && (it.state == player.player.other().symbolState)) // opponent
                    1.0
                else
                    0.0
            )
        }
    }
    val outputs = fire(inputs).last()

    val filtered = buildList {
        outputs.forEachIndexed { i, it ->
            val stack = i.slotIndex(player.depth)
            if (normal[stack].active) {
                add(Pair(stack, it.coerceAtLeast(0.0)))
            }
        }
    }.sortedByDescending { it.second }
    val total = filtered.sumOf { it.second }

    val normalSlot = if (total == 0.0) filtered.random().first
    else filtered.first().first

    val slot = transform.inverse(normalSlot)

    return Move(player.game, normal, inputs, slot, normalSlot)
}

fun GamePlayer.makeMove(slotIndex: SlotIndex) = makeMove(game.positionToMove(slotIndex))

data class Move(
    val originalGame: Game,
    val normalizedGame: Game,
    val encoded: List1d,
    val slot: SlotIndex,
    val normalSlot: SlotIndex
) {
    override operator fun equals(other: Any?): Boolean = other is Move && encoded == other.encoded && slot == other.slot
}

fun moveFromOriginal(player: GamePlayer, slot: SlotIndex) = moveFromOriginal(player.game, player.player, slot)


fun moveFromOriginal(originalGame: Game, player: Player, slot: SlotIndex): Move {
    val (normalized, transform) = originalGame.normal

    val inputs = buildList {
        normalized.foreach {
            add(
                if (it is Symbol && (it.state == player.symbolState)) // self
                    1.0
                else
                    0.0
            )

            add(
                if (it is Symbol && (it.state == player.other().symbolState)) // opponent
                    1.0
                else
                    0.0
            )
        }
    }

    return Move(originalGame, normalized, inputs, slot, transform(slot))
}

data class CorrectionSet(val game: Game, val inputs: List1d, val moveSet: MutableSet<SlotIndex>) {
    override fun toString() = moveSet.toString()
}

class GameMap {
    class TeacherMap(var x: LastGame?, var o: LastGame?) {
        data class LastGame(val history: List<Move>, val score: Double, val repeat: Boolean)

        val complete: Boolean get() = x != null && o != null
        val values get() = listOf(x, o)


        operator fun get(player: Player) = when (player) {
            Player.X -> x
            Player.O -> o
        }

        operator fun set(player: Player, value: LastGame) = when (player) {
            Player.X -> x = value
            Player.O -> o = value
        }
    }

    val map = mutableMapOf<Network, TeacherMap>()
    val values = map.values

    operator fun get(teacher: Network): TeacherMap {
        val indexed = map[teacher]
        return if (indexed == null) {
            val out = TeacherMap(null, null)
            map[teacher] = out
            out
        } else {
            indexed
        }
    }


}
