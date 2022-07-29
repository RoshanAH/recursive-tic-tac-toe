package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.Connection
import com.roshanah.rt3.client.rendering.*
import com.roshanah.rt3.client.scenes
import com.roshanah.rt3.client.zoom
import com.roshanah.rt3.selectorManager
import io.ktor.network.sockets.*
import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.launch
import org.openrndr.math.Matrix44

private lateinit var connection: Connection

fun mainMenu(program: Program) = buildMenu(program, Matrix44.IDENTITY) {
    button(3, player, DepthSelection(program, board.fromBoard * getTransform(3)) { depth, move ->
        val game = Singleplayer(program, transform, depth)
        game.renderer.game = game.player.game
        game.renderer.addQueue.forEach {
            it.development = 1.0
        }
        game.player.makeMove(move)
        game.renderer.game = game.player.game
        game
    })



    button(4, ai) {
        textFieldToScene(1) {
            Training(program, this.transform, it.toInt())
        }
    }


    button(5, multiplayer, {
        program.launch {
            while (!::connection.isInitialized) yield()
            val slot5 = board.fromBoard * getTransform(5)
            button(5, create, DepthSelection(program, slot5 * slot5) { depth, move ->
                val game = Multiplayer(program, transform, "$depth-", connection)
                connection.onReceived = { game.clientReceive(it) }
                game.renderer.game = game.player.game
                game.renderer.addQueue.forEach {
                    it.development = 1.0
                }
                program.launch {
                    println("Room number: ${connection.query("create:$depth")}")
                    game.tryMove(move)
                }
                game
            })
            button(3, join, {}, join@{
                program.launch {
                    while (true) {
                        try {
                            println("Input room number:")
                            val number = readLine()?.toInt()
                            val serial = connection.query("join:$number")
                            if (serial == "failed") {
                                println("Room not found")
                                continue
                            }
                            val transform = this@join.transform * getTransform(3)
                            val game = Multiplayer(program, transform, serial, connection)
                            println("Room found")
                            connection.onReceived = { game.clientReceive(it) }
                            scenes.add(game)
                            zoom(game.transform)
                            this@join.targetOpacity = 0.0
                            break
                        } catch (_: NumberFormatException) {
                            println("Invalid room number format")
                        }
                    }
                }
            })
        }
    }, {
        println("Connecting...")
//        TODO replace this with actual server ip
        program.launch {
            connection = Connection(aSocket(selectorManager).tcp().connect("127.0.0.1", 33584), this)
            connection.run()
            connection.onDisconnect = {
                println("Lost connection")
            }
            println("Successfully connected")
        }
    })
}



