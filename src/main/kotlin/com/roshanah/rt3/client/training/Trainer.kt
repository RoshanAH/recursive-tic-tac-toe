package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.elements.Game
import com.roshanah.rt3.client.elements.Player
import com.roshanah.rt3.ml.List1d
import com.roshanah.rt3.ml.Network
import kotlin.math.pow

// network output is: win, tie, loss

class Trainer(val depth: Int, network: Network) {

    var player = GamePlayer(depth)

    var teacher: Network = network
    var student: Network = network.random
    var studentPlayer = Player.X
        private set

    var state = State.PLAY
        private set

    var totalGamesPlayed = 0
        private set

    var learningRate = 0.1
    val moveSet = mutableSetOf<Pair<List1d, List1d>>()
    val currentGame = mutableListOf<List1d>()

    val winRatio = mutableListOf<Double>()
    var minWinRatio = 0.7

    constructor(depth: Int, layers: Int, nodes: Int) : this(
        depth,
        Network.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, layers, nodes, 3)
    )

    constructor(depth: Int) : this(
        depth,
        Network.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, depth + 2, (1..depth + 1).sumOf { 9.0.pow(it).toInt() }, 3)
    )

    fun update(){
        when (state) {
            State.PLAY -> {
                val gameState = player.game.evaluate()
                if(!gameState.finished) {
                    if (player.player == studentPlayer) {
                        player.makeMove(student.calculateMove(player))
                        currentGame += player.game.encode(studentPlayer)
                    }
                    else {
                        player.makeMove(teacher.calculateMove(player))
                    }
                } else {
                    winRatio.add(when {
                        gameState == Game.State.TIE -> 0.5
                        gameState.player == studentPlayer -> 1.0
                        else -> 0.0
                    })
                    if(winRatio.size >= 2) state = State.DESCENT
                    val key = when {
                        gameState == Game.State.TIE -> listOf(0.0, 1.0, 0.0)
                        gameState.player == studentPlayer -> listOf(1.0, 0.0, 0.0)
                        else -> listOf(0.0, 0.0, 1.0)
                    }
                    player = GamePlayer(depth)
                    currentGame.forEach { moveSet += Pair(it, key) }
                    currentGame.clear()
                    totalGamesPlayed++
                }
            }
            State.DESCENT -> {
                var sum = student.zero
                moveSet.forEach{
                    sum += student.computeGradient(it.first, it.second)
                }
                sum /= moveSet.size
                student = student.descend(sum, learningRate)

                state = if(winRatio.sum() / 2 > minWinRatio) State.REPLACEMENT
                else State.PLAY
                winRatio.clear()
            }
            State.REPLACEMENT -> {
                moveSet.clear()
                teacher = student
                student = student.random
            }
        }
    }

    enum class State {
        PLAY, DESCENT, REPLACEMENT
    }
}

fun Network.calculateMove(player: GamePlayer): Int {
    val possible = player.game.possibleMoves
    require(possible >= 1) {
        "Game has finished"
    }
    val game = player.game
    var bestMove = 0
    var bestOutcome: List1d = fire(game.getMove(0, player.player).encode(player.player)).last()
    for(i in 1 until possible){
        val outcome = fire(game.getMove(i, player.player).encode(player.player)).last()

        val noLoss = outcome.last() <= 0 && bestOutcome.last() <= 0
        val higherWin = outcome[0] > bestOutcome[0]
        val lowerLoss = outcome.last() < bestOutcome.last()
        if(noLoss) {
            if(higherWin){
                bestMove = i
                bestOutcome = outcome
            }
        } else if (lowerLoss) {
            bestMove = i
            bestOutcome = outcome
        }
    }

    return bestMove

}