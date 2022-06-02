package com.roshanah.recursiveTac.client.elements

class Evaluation(val game: Game, val move: Int, var xWins: Int, var oWins: Int, var ties: Int) {
    operator fun plus (other: Evaluation): Evaluation {
        return Evaluation(game, move, this.xWins + xWins, this.oWins + oWins, this.ties + ties)
    }
}