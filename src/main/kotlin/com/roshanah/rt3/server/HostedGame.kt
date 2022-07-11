package com.roshanah.rt3.server

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.Connection
import com.roshanah.rt3.client.elements.Player
import java.io.IOException


var numberLength = 3
class HostedGame(player1: Connection, initialGame: GamePlayer, player1Type: Player, number: Int) {
    var player1: Connection?
    var player1Type: Player
    var player2: Connection? = null
    var player2Type: Player? = null
    var game: GamePlayer
    val roomNumber: Int
    var full = false
    var onFinish: () -> Unit = { }

    init {
        game = initialGame
        this.player1 = player1
        this.player1Type = player1Type
        player1.onReceived = { s -> processPacket(s, player1Type, player1) }
        player1.onDisconnect = {
            if (player2 != null) {
                player2?.broadcast("info:Opponent has disconnected")
                player2?.broadcast("info:Room number: $number")
            } else {
                onFinish()
            }
            this.player1 = player2
            //      this.player2 = null;
            full = false
        }
        roomNumber = number
    }

    val number: String
        get() = String.format("%03d", roomNumber)

    private fun processPacket(s: String, playerType: Player?, player: Connection) {
        if (game.player !== playerType) {
            player.broadcast("return:invalid")
            return
        }
        val existing: String = game.serialize()
        if (existing != s.substring(0, existing.length)) {
            player.broadcast("info:mismatched history")
            player.broadcast("return:invalid")
            return
        }
        try {
            val thisSerial: String = game.serialize()
            val move = s.substring(thisSerial.length).toInt()
            game.makeMove(move)
        } catch (e: IllegalArgumentException) {
            player.broadcast("info:" + e.message)
            player.broadcast("return:invalid")
            return
        }
        player.broadcast("return:valid")
        if (player1 !== player && player1 != null) player1?.broadcast("game:$s")
        if (player2 !== player && player2 != null) player2?.broadcast("game:$s")
        if (!game.game.active) {
            try {
                player1?.socket?.close()
                player2?.socket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            onFinish()
        }
    }

    fun connect(number: String, player2: Connection): Boolean {
        if (full) return false
        return if (number == roomNumber.toString()) {
            this.player2 = player2
            player2Type = player1Type.other()
            player2.onReceived = { s -> processPacket(s, player2Type, player2) }
            player2.onDisconnect = {
                player1?.broadcast("info:Opponent has disconnected")
                player1?.broadcast("info:Room number: $number")
                full = false
            }
            full = true
            true
        } else false
    }
}