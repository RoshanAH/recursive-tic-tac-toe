package com.roshanah.recursiveTac.client.elements

enum class Player {
    X, O;

    fun other(): Player {
        return when (this) {
            X -> O
            O -> X
        }
    }

    val symbol: Symbol
        get() = when (this) {
            X -> x()
            O -> o()
        }

    val symbolState: Symbol.State
        get() = when (this){
            X -> Symbol.State.X
            O -> Symbol.State.O
        }
}