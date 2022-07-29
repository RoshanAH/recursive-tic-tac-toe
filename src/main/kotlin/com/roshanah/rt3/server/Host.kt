package com.roshanah.rt3.server

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.Connection
import com.roshanah.rt3.client.SERVER_IP
import com.roshanah.rt3.client.elements.Player
import com.roshanah.rt3.recieve
import com.roshanah.rt3.selectorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.Throws
import java.io.IOException
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import java.util.*
import kotlin.math.pow

class Host {
    private val idles: MutableSet<Connection> = HashSet<Connection>()
    private val games: MutableSet<HostedGame> = HashSet<HostedGame>()
    private val server = aSocket(selectorManager).tcp().bind("127.0.0.1", 33584)

    @Throws(IOException::class)
    internal fun run(scope: CoroutineScope) = scope.launch {
        while (true) {
            val c = Connection(server.accept(), this)
            c.run()
            c.onReceived = { s ->
                handleIdleCommand(c, s)
            }
            c.onDisconnect = {
                println("connection left")
                idles.remove(c)
            }
            idles.add(c)
            c.broadcast("info:Connected")
            println(idles.size.toString() + " idle connection" + if (idles.size == 1) "" else "s")
        }
    }

    private fun handleIdleCommand(c: Connection, s: String) = recieve(s) {
        prefix("join") {
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
        prefix("create") {
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
        prefix("load") {
            val split = command.split(",")
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
        prefix("command") {
            prefix("exit") {
                try {
                    c.socket.close()
                } catch (e: IOException) {
                }
                idles.remove(c)
            }
        }
    }
}


fun main(): Unit = runBlocking {
    try {
        Host().run(this)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}