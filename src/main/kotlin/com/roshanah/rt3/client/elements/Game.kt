package com.roshanah.rt3.client.elements

import java.lang.IllegalArgumentException
import java.util.ArrayList
import kotlin.math.pow

class Game : Slot {
    val slots: Array<Array<Slot?>>
    val depth: Int

    constructor(depth: Int) {
        this.depth = depth
        active = true
        slots = Array(3) { arrayOfNulls(3) }
        iterate { r: Int, c: Int ->
            if (depth > 0) {
                slots[r][c] = Game(depth - 1)
            } else {
                slots[r][c] = blank()
            }
        }
        activateSlots(ArrayList())
    }

    operator fun get(index: Int) = slots[index]

    override fun clone(): Game {
        val newSlots = Array(3) { arrayOfNulls<Slot>(3) }
        iterate { r: Int, c: Int -> newSlots[r][c] = slots[r][c]!!.clone() }
        return Game(newSlots, depth)
    }

    constructor(slots: Array<Array<Slot?>>, depth: Int) {
        this.depth = depth
        this.slots = slots
    }

    private fun getMove(move: Int, p: Player, moveStack: MutableList<Int>): Game {
        var move = move
        val totalMoves = possibleMoves
        require(totalMoves > 0) { "Attempted move $move on finished game" }
        require(!(move < 0 || move >= totalMoves)) { "move " + move + " not in range [0, " + (totalMoves - 1) + "]" }
        var slotIndex = 0
        var newSlot: Slot? = null
        row@ for (r in 0..2) for (c in 0..2) {
            slotIndex = r * 3 + c
            val s = slots[r][c]
            val moves = s!!.possibleMoves
            if (moves <= move) {
                move -= moves
            } else {
                moveStack.add(slotIndex)
                newSlot = if (s is Symbol) {
                    p.symbol
                } else {
                    val subGame = (s as Game?)!!.getMove(move, p, moveStack)
                    val evaluation = subGame.evaluate()
                    if (evaluation.hasWinner) {
                        when (evaluation) {
                            State.X -> x()
                            State.O -> o()
                            else -> throw IllegalArgumentException("Won game has no winner")
                        }
                    } else subGame
                }
                break@row
            }
        }
        val finalSlotIndex = slotIndex
        val finalNewSlot = newSlot
        val newSlots = Array(3) { arrayOfNulls<Slot>(3) }
        iterate { r: Int, c: Int ->
            if (r * 3 + c == finalSlotIndex) newSlots[r][c] = finalNewSlot else newSlots[r][c] = slots[r][c]!!.clone()
        }
        return Game(newSlots, depth)
    }

    fun getSlot(slotStack: List<Int>): Slot {
        var out: Slot = this
        for (slot in slotStack) {
            if (out !is Game) break
            out = out[slot / 3][slot % 3]!!
        }

        return out
    }

    operator fun set(index: List<Int>, value: Slot) {
        var current: Slot = this
        var i = 0
        while (i in 0 until index.size - 1) {
            val slot = index[i]
            if (current !is Game) break
            current = current[slot / 3][slot % 3]!!
            i++
        }

        require(current is Game){
            "Cannot modify Game, indexed element must be Symbol"
        }

        val slot = index[i]
        current[slot / 3][slot % 3] = value
    }

    operator fun set(index: SlotIndex, value: Slot) = set(index.stack, value)

    operator fun get(slot: SlotIndex) = getSlot(slot.stack)
    operator fun get(slot: List<Int>) = getSlot(slot)

    fun getMove(move: Int, p: Player): Game {
        val moveStack: MutableList<Int> = ArrayList()
        val game = getMove(move, p, moveStack)
        moveStack.removeAt(0)
        game.activateSlots(moveStack)
        if (game.evaluate().finished) game.active = false
        return game
    }

    private fun internalPositionOf(move: Int): List<Int> {

        var move = move
        for (r in 0 until 3) for (c in 0 until 3) {
            val slot = this[r][c] ?: continue
            val possible = slot.possibleMoves
            if (possible == 0) continue
            if (move >= possible) {
                move -= possible
                continue
            }

            return if (slot is Game)
                listOf(3 * r + c) + slot.internalPositionOf(move)
            else
                listOf(3 * r + c + move)
        }

        error("No move found in positionOf function")
    }

    fun positionOf(move: Int): SlotIndex = internalPositionOf(move).slotIndex

    fun positionToMove(slot: List<Int>): Int {
        if (slot.isEmpty()) return 0

        var total = 0

        for (i in 0 until slot[0]) {
            total += (slots[i / 3][i % 3] ?: continue).possibleMoves
        }

        return when (val currentSlot = slots[slot[0] / 3][slot[0] % 3]) {
            is Game -> total + currentSlot.positionToMove(slot.subList(1, slot.size))
            is Symbol -> total
            else -> 0
        }
    }

    fun positionToMove(slot: SlotIndex) = positionToMove(slot.stack)

    fun foreachIndexed(action: (SlotIndex, Slot) -> Unit) {
        val slotStack = mutableListOf(0)
        while (true) {
            action(slotStack.slotIndex, this[slotStack])
            if (slotStack.size < depth + 1) {
                slotStack.add(0)
            } else {
                while (slotStack.lastOrNull() == 8) slotStack.removeLast()
                if (slotStack.isEmpty()) break
                slotStack[slotStack.lastIndex]++
            }
        }
    }

    fun foreach(action: (Slot) -> Unit) = foreachIndexed { _, s -> action(s) }

    override fun activateSlots(moveStack: MutableList<Int>): Boolean {
        if (moveStack.isEmpty()) {
            var success = false
            for (r in slots) for (c in r) {
                if (c!!.activateSlots(ArrayList())) success = true
            }
            active = success
            return success
        }
        val slot: Int = moveStack.removeAt(0)
        require(!(slot < 0 || slot > 8)) { "Slot $slot not in range [0, 8]" }
        return if (!slots[slot / 3][slot % 3]!!.activateSlots(moveStack)
                .also { active = it }
        ) activateSlots(ArrayList()) else true
    }

    fun evaluate(): State {
        for (r in 0..2) {
            val check = check3(slots[r][0], slots[r][1], slots[r][2])
            if (check.hasWinner) return check
        }
        for (c in 0..2) {
            val check = check3(slots[0][c], slots[1][c], slots[2][c])
            if (check.hasWinner) return check
        }
        val diagonal1 = check3(slots[0][0], slots[1][1], slots[2][2])
        if (diagonal1.hasWinner) return diagonal1
        val diagonal2 = check3(slots[0][2], slots[1][1], slots[2][0])
        if (diagonal2.hasWinner) return diagonal2
        return if (possibleMoves == 0) State.TIE else State.ONGOING
    }

    private fun check3(first: Slot?, second: Slot?, third: Slot?): State {
        val firstS: Symbol
        val secondS: Symbol
        val thirdS: Symbol
        if (first is Symbol && second is Symbol && third is Symbol) {
            firstS = first
            secondS = second
            thirdS = third
        } else {
            return State.ONGOING
        }
        return if (firstS.state == secondS.state && secondS.state == thirdS.state && firstS.state != Symbol.State.BLANK)
            when (firstS.state) {
                Symbol.State.X -> State.X
                Symbol.State.O -> State.O
                else -> error("")
            } else State.ONGOING
    }

    override val possibleMoves: Int
        get() {
            if (!active) return 0
            var total = 0
            for (c in slots) for (s in c) total += s?.possibleMoves ?: 0
            return total
        }

    fun iterate(e: (Int, Int) -> Unit) {
        for (r in 0..2) for (c in 0..2) e(r, c)
    }



    enum class State(val finished: Boolean, val hasWinner: Boolean) {
        X(true, true),
        O(true, true),
        TIE(true, false),
        ONGOING(false, false);

        val player: Player?
            get() = when (this) {
                X -> Player.X
                O -> Player.O
                else -> null
            }
    }
}


data class SlotIndex internal constructor(val stack: List<Int>) {
    val depth = stack.size - 1
    val int: Int

    init {
        var int = 0
        stack.forEachIndexed { i, it ->
            int += it * 9.0.pow(depth - i).toInt()
        }
        this.int = int
    }

    operator fun plus(other: SlotIndex) : SlotIndex {
        require (depth == other.depth){
            "Depth of $depth does not match other depth of ${other.depth}"
        }
        return (int + other.int).slotIndex
    }

    operator fun minus(other: SlotIndex) : SlotIndex {
        require (depth == other.depth){
            "Depth of $depth does not match other depth of ${other.depth}"
        }
        return (int - other.int).slotIndex
    }

    operator fun compareTo(other: SlotIndex) : Int {
        require (depth == other.depth){
            "Depth of $depth does not match other depth of ${other.depth}"
        }
        return int.compareTo(other.int)
    }
}

val List<Int>.slotIndex get() = SlotIndex(this)

val Int.slotIndex: SlotIndex get() = SlotIndex(buildList {
            var value = this@slotIndex
            var current: Int = value
            do {
                val rem = current % 9
                val quotient = current / 9
                add(quotient)
            } while (rem != 0)
        }
)


