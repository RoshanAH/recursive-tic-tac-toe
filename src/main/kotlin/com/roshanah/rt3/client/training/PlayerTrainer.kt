package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.elements.*
import com.roshanah.rt3.ml.List1d

import com.roshanah.rt3.ml.Network
import kotlin.math.pow


class PlayerTrainer(val depth: Int, network: Network) {

    var player = AmbiguousPlayer(GamePlayer(depth))
    private set

    var teacher: Network = network
    var student: Network = network.random
    var studentPlayer = Player.X
        private set

    var state = State.PLAY
        private set

    var totalGamesPlayed = 0
        private set

    var learningRate = 0.1
    val moveSet = mutableSetOf<Pair<List<Double>, List<Double?>>>()

    val winRatio = mutableListOf<Double>()
    var minWinRatio = 0.7

    private data class CorrectionState(val game: Game, val moveSet: MutableSet<SlotIndex>){
        override fun toString() = moveSet.toString()
    }

    private val correctionStack = mutableListOf<CorrectionState>()
//    A list of network outputs, for each player
    private val lastGames = mutableMapOf<Player, List<SlotIndex>>()

    fun update() {
        when (state) {
            State.PLAY -> {
                if(!player.original.player.game.evaluate().finished) {
                    if (player.original.player.player == studentPlayer)
                        makeMove(student.fire(player.normalized.encode(studentPlayer)).last())
                    else
                        makeMove(teacher.fire(player.normalized.encode(studentPlayer.other())).last())
                }

                val evaluation = player.original.player.game.evaluate()
                if (evaluation.finished) {

                    state = if (evaluation.player == studentPlayer) {
                        if (correctionStack.isEmpty()) winRatio += 1.0 // win
                        State.WIN
                    } else {
                        if (correctionStack.isEmpty()) {
                            winRatio += if (evaluation.player == studentPlayer.other()) 0.0 // loss
                            else 0.5 // tie
                        }
                        State.CORRECTION
                    }

                    if(correctionStack.isEmpty()) {
                         lastGames[studentPlayer] = player.moveHistory.filterIndexed { i, _ ->
                             (studentPlayer == Player.X) == (i % 2 == 0)
                         }
                    }
                }

            }
            State.WIN -> {
                for(move in player.extractMoves(studentPlayer)){
                    moveSet += Pair(move.first.encode(studentPlayer), move.first.encodeKey(move.second))
                    moveSet
                }
                player = AmbiguousPlayer(GamePlayer(depth))
                studentPlayer = studentPlayer.other()
                correctionStack.clear()
                totalGamesPlayed++
                state = if (winRatio.size < 2) State.PLAY else State.DESCENT
            }
            State.CORRECTION -> {
                val studentMoves = player.extractMoves(studentPlayer)
//                println(player.original.player.moveHistory.zip(player.original.player.history))
                for (i in correctionStack.size until  studentMoves.size){
                    correctionStack.add(CorrectionState(studentMoves[i].first, mutableSetOf(studentMoves[i].second)))
                }

//                println(correctionStack.last().moveSet)


//                println(correctionStack)

                while (correctionStack.last().moveSet.size >= correctionStack.last().game.possibleMoves) {
                    correctionStack.removeLast()
                    if(correctionStack.isEmpty()) {
                        player = AmbiguousPlayer(GamePlayer(depth))
                        studentPlayer = studentPlayer.other()
                        correctionStack.clear()
                        totalGamesPlayed++
                        state = if (winRatio.size < 2) State.PLAY else State.DESCENT
                        return
                    }
                }

//                  Undo the game so the student gets another chance to make a move
                player.original.player.currentIndex = (correctionStack.size - 1) * 2 + if(studentPlayer == Player.X) 0 else 1
                player = AmbiguousPlayer(player.original.player.fork())

                val last = correctionStack.last()
                val moveChoices = buildList {
                    student.fire(last.game.encode(studentPlayer)).last().forEachIndexed { i, value ->
                        val stack = i.slotIndex
                        if (last.game[stack].active)
                            add(Pair(stack, value))
                    }
                }.sortedByDescending { it.second }

                for(move in moveChoices){
                    if(!last.moveSet.contains(move.first)) {
                        player.makeMove(move.first)
                        last.moveSet.add(move.first)
                        state = if (winRatio.size < 2) State.PLAY else State.DESCENT
                        break
                    }
                }

            }
            State.DESCENT -> {
                var avg = student.zero
                moveSet.forEach { avg += student.computeGradient(it.first, it.second) }
                avg /= moveSet.size.toDouble()

//                println(avg.formattedString())

                state = if (winRatio.sum() / winRatio.size >= minWinRatio && winRatio.size >= 2) {
                    State.REPLACEMENT
                } else {
                    State.PLAY
                }

                student = student.descend(avg, learningRate)
                winRatio.clear()
            }
            State.REPLACEMENT -> {
                teacher = student
                student = student.random
                state = State.PLAY
                moveSet.clear()
            }
        }
    }


    constructor(depth: Int, layers: Int, nodes: Int) : this(
        depth,
        Network.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, layers, nodes, 9.0.pow(depth + 1).toInt())
    )


    private fun makeMove(outputs: List1d) {
        val filtered = buildList {
            outputs.forEachIndexed { i, it ->
                val stack = getStack(i)
                if (player.normalized.getSlot(stack).active) {
                    add(Pair(stack, it.coerceAtLeast(0.0)))
                }
            }
        }.sortedByDescending { it.second }
        val total = filtered.sumOf { it.second }

        if(total == 0.0) player.makeMove(filtered.random().first)
        else player.makeMove(filtered.first().first)
    }

    enum class State {
        PLAY, CORRECTION, WIN, DESCENT, REPLACEMENT
    }

}

//    One hot encoding
fun Game.encode(player: Player) = buildList {
    val slotStack = mutableListOf(0)
    while (true) {
        val slot = this@encode[slotStack]
        add (
            if (slot is Symbol &&  (slot.state == player.symbolState)) // self
                1.0
            else
                0.0
        )

        add (
            if (slot is Symbol && (slot.state == player.other().symbolState)) // opponent
                1.0
            else
                0.0
        )

        if (slotStack.size < depth + 1) {
            slotStack.add(0)
        } else {
            while (slotStack.lastOrNull() == 8) slotStack.removeLast()
            if (slotStack.isEmpty()) break
            slotStack[slotStack.size - 1]++
        }
    }
}

private fun Game.encodeKey(slot: SlotIndex): List<Double?> {
    val currentSlot = mutableListOf<Int>()
    val out = mutableListOf<Double?>()

    do {
        while (currentSlot.size < depth + 1) currentSlot += 0
        val it = this[currentSlot]
        out += when {
            currentSlot.containsAll(slot.stack) -> 1.0
            it.active -> 0.0
            else -> null
        }
        currentSlot[currentSlot.size - 1]++
        while (currentSlot.last() == 9) {
            currentSlot.removeLast()
            if (currentSlot.isEmpty()) break
            currentSlot[currentSlot.size - 1]++
        }
    } while (currentSlot.isNotEmpty())

    return out
}

