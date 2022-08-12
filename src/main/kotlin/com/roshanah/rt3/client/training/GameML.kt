package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.elements.*

class Transform(
    private val transform: (SlotIndex) -> SlotIndex,
    val inverse: (SlotIndex) -> SlotIndex
) : (SlotIndex) -> SlotIndex {
    override fun invoke(input: SlotIndex) = transform(input)
}

private val i: (SlotIndex) -> SlotIndex = { it }

private val c: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        3 * (slot % 3) - slot / 3 + 2
    }.slotIndex
}

private val cc: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        6 - 3 * (slot % 3) + slot / 3
    }.slotIndex
}

private val r2: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        8 - 3 * (slot / 3) - slot % 3
    }.slotIndex
}

private val vm: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        3 * (slot / 3) - slot % 3 + 2
    }.slotIndex
}

private val hm: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        6 - 3 * (slot / 3) + slot % 3
    }.slotIndex
}

private val d1: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        3 * (slot % 3) + slot / 3
    }.slotIndex
}

private val d2: (SlotIndex) -> SlotIndex = {
    it.stack.map { slot ->
        8 - 3 * (slot % 3) - slot / 3
    }.slotIndex
}

val identity = Transform(i, i)
val clockwise = Transform(c, cc)
val counterClockwise = Transform(cc, c)
val rotateTwice = Transform(r2, r2)
val verticalMirror = Transform(vm, vm)
val horizontalMirror = Transform(hm, hm)
val diagnol1Mirror = Transform(d1, d1)
val diagnol2Mirror = Transform(d2, d2)

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

fun Game.transform(transform: (SlotIndex) -> SlotIndex): Game {
    val new = clone()
    foreachIndexed { i, it ->
        new[transform(i)] = it
    }
    return new
}

operator fun ((SlotIndex) -> SlotIndex).invoke(game: Game) = game.transform(this)

val Game.normalize: Game
    get() {
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
        transforms.forEach { if (it < smallest) smallest = it }
        return smallest
    }

val Game.normal: Pair<Game, Transform>
    get() {
        val transforms = listOf(
            Pair(transform(clockwise), clockwise),
            Pair(transform(counterClockwise), counterClockwise),
            Pair(transform(rotateTwice), rotateTwice),
            Pair(transform(verticalMirror), verticalMirror),
            Pair(transform(horizontalMirror), horizontalMirror),
            Pair(transform(diagnol1Mirror), diagnol1Mirror),
            Pair(transform(diagnol2Mirror), diagnol2Mirror),
        )
        var smallest = Pair(this, identity)
        transforms.forEach { if (it.first < smallest.first) smallest = it }
        return smallest
    }

//    One hot encoding
fun Game.encode(player: Player) = buildList {
    normalize.foreach {
        add(
            if (it is Symbol && (it.state == player.symbolState)) // self
                1.0
            else
                0.0
        )

        add(
            if (it is Symbol && (it.state == player.other().symbolState)) // opponent
                1.0
            else
                0.0
        )
    }
}

fun Game.rawEncode(player: Player) = buildList {
    foreach {
        add(
            if (it is Symbol && (it.state == player.symbolState)) // self
                1.0
            else
                0.0
        )

        add(
            if (it is Symbol && (it.state == player.symbolState)) // opponent
                1.0
            else
                0.0
        )
    }
}

