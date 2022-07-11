package com.roshanah.rt3.client.elements

class Symbol internal constructor(val state: State) : Slot() {
    init {
        active = state.active
    }

    override fun clone(): Slot {
        return Symbol(state)
    }

    override val possibleMoves: Int
        get() = if (active) 1 else 0

    override fun activateSlots(slotStack: MutableList<Int>): Boolean {
        active = state.active
        return active
    }

    enum class State(val active: Boolean) {
        BLANK(true), X(false), O(false);
    }
}

fun blank(): Symbol {
    return Symbol(Symbol.State.BLANK)
}

fun x(): Symbol {
    return Symbol(Symbol.State.X)
}

fun o(): Symbol {
    return Symbol(Symbol.State.O)
}