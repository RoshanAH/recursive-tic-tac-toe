package com.roshanah.rt3

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.net.SocketException
import kotlin.coroutines.CoroutineContext



val selectorManager = SelectorManager(Dispatchers.IO)
class Connection private constructor(
    val socket: Socket,
    val scope: CoroutineScope?,
    val dispatcher: CoroutineContext?
) {

    constructor(socket: Socket, scope: CoroutineScope) : this(socket, scope, null)
    constructor(socket: Socket, dispatcher: CoroutineContext) : this(socket, null, dispatcher)

    private val output = socket.openWriteChannel(autoFlush = true)
    private val input = socket.openReadChannel()

    var onDisconnect: () -> Unit = { }
    var onReceived: (String) -> Unit = { }
    private var queryRequested = false
    private var response = ""

    fun broadcast(s: String) = launch {
        output.writeStringUtf8("$s\n")
    }

    suspend fun suspendedWrite(s: String) = output.writeStringUtf8("$s\n")

    suspend fun query(s: String): String {
        output.writeStringUtf8("$s\n")
        queryRequested = true
        while (queryRequested) yield()
        return response
    }

    fun run() = launch {
        try {
            while (!socket.isClosed) {
//                println("reading next input")
                val input = input.readUTF8Line() ?: break
//                println("received: $input")
                receive(input)
            }
        } catch (e: SocketException){
            socket.close()
        }
        onDisconnect()
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

     fun launch(function: suspend CoroutineScope.() -> Unit) = scope?.launch(block=function)
        ?: if (dispatcher != null) GlobalScope.launch(dispatcher, block = function)
        else error("")
}
