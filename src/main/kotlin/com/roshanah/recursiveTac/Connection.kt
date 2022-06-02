package com.roshanah.recursiveTac

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.io.PrintStream
import java.net.Socket
import java.util.*

class Connection(val socket: Socket) {
    private val output: PrintStream = PrintStream(socket.getOutputStream())
    private val input: Scanner = Scanner(socket.getInputStream())
    var job: Job? = null
    val connected
        get() = job?.isActive ?: false
    var onDisconnect: () -> Unit = { }
    var onReceived: (String) -> Unit = { }
    private var queryRequested = false
    private var response = ""

    fun broadcast(s: String) {
        output.println(s)
    }

    suspend fun query(s: String): String {
        output.println(s)
        queryRequested = true
        while (queryRequested) yield()
        return response
    }

    fun run(): Job{
        var j: Job
        runBlocking {
            j = launch {
                while (socket.isConnected && input.hasNext()) receive(input.nextLine())
                onDisconnect()
            }
        }
        job = j
        return j
    }

    private fun receive(s: String) {
        if (queryRequested) {
            val splitIndex = s.indexOf(":")
            val commandType = s.substring(0, splitIndex)
            val command = s.substring(splitIndex + 1)
            if (commandType == "return") {
                queryRequested = false
                response = command
                return
            }
        }
        onReceived(s)
    }
}

//class Reciever(val type: String?) {
//    val children
//
//    private constructor(type: String, vararg recievers: Reciever) : this(type){
//
//    }
//
//}

class RecieverBuilder(val command: String){



    fun command(type: String, body: RecieverBuilder.() -> Unit){

    }
}