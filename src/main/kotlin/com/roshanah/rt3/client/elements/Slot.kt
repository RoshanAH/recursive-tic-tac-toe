package com.roshanah.rt3.client.elements

abstract class Slot {
    open var active = false
    abstract val possibleMoves: Int
    abstract fun activateSlots(slotStack: MutableList<Int>): Boolean
    abstract fun clone(): Slot
}