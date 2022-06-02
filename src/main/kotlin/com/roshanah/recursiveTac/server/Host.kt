package com.roshanah.recursiveTac.server

import com.roshanah.recursiveTac.client.GamePlayer
import com.roshanah.recursiveTac.Connection
import com.roshanah.recursiveTac.client.elements.Player
import java.net.ServerSocket
import kotlin.Throws
import java.io.IOException
import java.util.*
import kotlin.math.pow

class Host {
    private val idles: MutableSet<Connection> = HashSet<Connection>()
    private val games: MutableSet<HostedGame> = HashSet<HostedGame>()
    private val server = ServerSocket(33584)

    @Throws(IOException::class)
    internal fun run() {
        while (true) {
            val c = Connection(server.accept())
            c.run()
            c.onReceived = { s -> handleIdleCommand(c, s) }
            c.onDisconnect = { println("connection left") }
            idles.add(c)
            c.broadcast("info:Connected")
            println(idles.size.toString() + " idle connection" + if (idles.size == 1) "" else "s")
        }
    }

    private fun handleIdleCommand(c: Connection, s: String) {
        val splitIndex = s.indexOf(":")
        val commandType = s.substring(0, splitIndex)
        val command = s.substring(splitIndex + 1)
        when (commandType) {
            "join" -> {
                var foundGame = false
                var connectedGame: HostedGame? = null
                for (game in games) {
                    if (game.connect(command, c)) {
                        connectedGame = game
                        idles.remove(c)
                        println("idle connection joined game")
                        foundGame = true
                        break
                    }
                }
                if (foundGame && connectedGame != null) c.broadcast("return:" + connectedGame.game.serialize()) else c.broadcast(
                    "return:failed"
                )
            }
            "create" -> {
                val gameCapacity = 10.0.pow(numberLength.toDouble()).toInt()
                val bound = gameCapacity - games.size
                var number = Random().nextInt(bound)
                while (true) {
                    var unique = true
                    for (g in games) if (g.roomNumber == number) {
                        number++
                        number %= gameCapacity
                        unique = false
                        break
                    }
                    if (unique) break
                }
                val depth = command.toInt()
                val game = HostedGame(c, GamePlayer(depth), Player.X, number)
                game.onFinish = { games.remove(game) }
                games.add(game)
                idles.remove(c)
                c.broadcast("return: " + game.number)
                println("new game created")
            }
            "load" -> {
                val split = command.split(",").toTypedArray()
                val bound = 1000 - games.size
                var number = Random().nextInt(bound)
                while (true) {
                    var unique = true
                    for (g in games) if (g.roomNumber == number) {
                        number++
                        number %= 10.0.pow(numberLength).toInt()
                        unique = false
                        break
                    }
                    if (unique) break
                }
                val p: Player = if (split[1] == "X") Player.X else Player.O
                val game = HostedGame(c, GamePlayer(split[0]), p, number)
                game.onFinish = { games.remove(game) }
                games.add(game)
                idles.remove(c)
                c.broadcast("return: " + game.number)
                println("new game created")
            }
            "command" -> {
                when (command) {
                    "exit" -> {
                        try {
                            c.socket.close()
                        } catch (e: IOException) {
                        }
                        idles.remove(c)
                    }
                    else -> c.broadcast("info:unknown command")
                }
            }
            else -> c.broadcast("info:unknown command type")
        }
    }

}

fun main() {
    try {
        Host().run()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}