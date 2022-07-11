package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.elements.Player
import com.roshanah.rt3.ml.Network
import kotlin.math.pow

class StateEvalTrainer(val depth: Int, network: Network) {

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

    constructor(depth: Int, layers: Int, nodes: Int) : this(
        depth,
        Network.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, layers, nodes, 9.0.pow(depth + 1).toInt())
    )

    fun update(){
        when (state) {
            State.PLAY -> {
                if(!player.original.player.game.evaluate().finished) {
                    if (player.original.player.player == studentPlayer)
                        makeMove(student, studentPlayer)
                    else
                        makeMove(teacher, !studentPlayer)
                }
            }
            State.DESCENT -> {

            }
            State.REPLACEMENT -> {

            }
        }
    }

    fun makeMove(network: Network, player: Player) {
        require(player == this.player.original.player.player) {
            "It it is not the network's turn"
        }
        val possible = this.player.original.player.game.possibleMoves
        require(possible >= 1) {
            "Game has finished"
        }


    }

    enum class State {
        PLAY, DESCENT, REPLACEMENT
    }
}