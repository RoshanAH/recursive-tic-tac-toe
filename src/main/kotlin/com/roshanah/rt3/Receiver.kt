package com.roshanah.rt3

class Receiver(val command: String) {
    fun prefix(prefix: String, body: Receiver.() -> Unit) {
        if(command.indexOf(prefix) != 0) return
        val subReceiver = Receiver(command.substring(prefix.lastIndex + 2)) // skip colon
        subReceiver.body()
    }
}
fun recieve(command: String, body: Receiver.() -> Unit) = Receiver(command).body()
