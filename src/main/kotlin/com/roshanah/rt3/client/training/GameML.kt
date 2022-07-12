package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.elements.*

private typealias Transform = (SlotIndex) -> SlotIndex

val identity: Transform = { it }

val clockwise: Transform = {
    it.stack.map { slot ->
        3 * (slot % 3) - slot / 3 + 2
    }.slotIndex
}

val counterClockwise: Transform = {
    it.stack.map { slot ->
        6 - 3 * (slot % 3) + slot / 3
    }.slotIndex
}

val rotateTwice: Transform = {
    it.stack.map { slot ->
        8 - 3 * (slot / 3) - slot % 3
    }.slotIndex
}

val verticalMirror: Transform = {
    it.stack.map { slot ->
        3 * (slot / 3) - slot % 3 + 2
    }.slotIndex
}

val horizontalMirror: Transform = {
    it.stack.map { slot ->
        6 - 3 * (slot / 3) + slot % 3
    }.slotIndex
}

val diagnol1Mirror: Transform = {
    it.stack.map { slot ->
        3 * (slot % 3) + slot / 3
    }.slotIndex
}

val diagnol2Mirror: Transform = {
    it.stack.map { slot ->
        8 - 3 * (slot % 3) - slot / 3
    }.slotIndex
}

operator fun Game.compareTo(other: Game): Int {

    for (r in 0 until 3) {
        for (c in 0 until 3) {
            val thisSlot = this[r][c]
            val otherSlot = other[r][c]

            if (thisSlot == null && otherSlot == null)
                continue
            if (thisSlot is Symbol && otherSlot is Symbol) {
                if (thisSlot.state == otherSlot.state)
                    continue

                val value: Symbol.() -> Int = {
                    when (this.state) {
                        Symbol.State.BLANK -> 0
                        Symbol.State.O -> 1
                        Symbol.State.X -> 2
                    }
                }

                return thisSlot.value().compareTo(otherSlot.value())
            }

            if (thisSlot is Symbol)
                return 1

            if (thisSlot !is Symbol && otherSlot is Symbol)
                return -1

            if (thisSlot is Game && otherSlot is Game) {
                val compare = thisSlot.compareTo(otherSlot)
                if (compare != 0) return compare
            }
        }
    }

    return 0
}

fun Game.transform(transform: Transform): Game {
    val new = clone()
    foreachIndexed { i, it ->
        new[transform(i)] = it
    }
    return new
}

val Game.normalize: Game get() {
    val transforms = listOf(
        transform(clockwise),
        transform(counterClockwise),
        transform(rotateTwice),
        transform(verticalMirror),
        transform(horizontalMirror),
        transform(diagnol1Mirror),
        transform(diagnol2Mirror),
    )
    var smallest = this
    transforms.forEach { if(it < smallest) smallest = it }
    return smallest
}

//    One hot encoding
fun Game.encode(player: Player) = buildList {
    normalize.foreach {
        add (
            if (it is Symbol && (it.state == player.symbolState)) // self
                1.0
            else
                0.0
        )

        add (
            if (it is Symbol && (it.state == player.other().symbolState)) // opponent
                1.0
            else
                0.0
        )
    }
}

