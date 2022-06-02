package com.roshanah.recursiveTac.client.rendering

import com.roshanah.recursiveTac.client.elements.Game
import com.roshanah.recursiveTac.client.elements.Symbol
import com.roshanah.recursiveTac.client.foreground
import com.roshanah.recursiveTac.client.renderedObjects
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import java.lang.IllegalStateException
import kotlin.math.*

private typealias GameSlot = com.roshanah.recursiveTac.client.elements.Slot

class GameRenderer(pos: Vector2, size: Double, val program: Program) {
    val objects: MutableList<Slot> = mutableListOf()
    val deleteQueue: MutableSet<String> = mutableSetOf()
    val addQueue: MutableSet<Slot> = mutableSetOf()
    val blanks: MutableList<Blank> = mutableListOf()
    var mouse: Vector2? = null
    var hoveredMove: Int? = null
        private set
    var showMoves = false

    lateinit var fromBoard: Matrix44
        private set
    lateinit var toBoard: Matrix44
        private set

    var game: Game? = null
        set(value) {
            blanks.clear()
            if (value == null) {
                deleteQueue.add("")
            } else {
                merge(field, value, true, listOf())
            }
            field = value
        }

    constructor(x: Double, y: Double, size: Double, program: Program) : this(Vector2(x, y), size, program)

    init {
        resize(pos, size)
    }

    private fun merge(old: GameSlot?, new: GameSlot?, active: Boolean, stack: List<Int>) {
        var stackStr = ""
        stack.forEach { stackStr += it }

        if (new is Game) {
            if (old !is Game) addQueue.add(createSlot(stack, SlotType.BOARD))
            for (r in 0 until 3) for (c in 0 until 3) {
                val newSlot = new[r][c]
                val oldSlot = if (old is Game) old[r][c] else null
                if (oldSlot == null) deleteQueue.add(stackStr)
                val newStack = mutableListOf<Int>().apply {
                    addAll(stack)
                    add(3 * r + c)
                }
                merge(oldSlot, newSlot, new.active, newStack)
            }
        } else if (new is Symbol) {
            if (active && new.state == Symbol.State.BLANK)
                blanks.add(Blank(getTransform(stack), 0.0))

            if ((old is Symbol && old.state != new.state) || old is Game || old == null) {
                if (!(old is Symbol && old.state == Symbol.State.BLANK || old == null)) deleteQueue.add(stackStr)
                if (new.state != Symbol.State.BLANK) addQueue.add(createSlot(stack, new.state.rendered))
            }
        } else if (new == null) {
            deleteQueue.add(stackStr)
        }
    }

    fun resize(pos: Vector2, size: Double) {
        fromBoard = transform {
            translate(pos + Vector2.ONE * padding * size * 0.5)
            scale(size * (1 - padding))
        }
        toBoard = fromBoard.inversed
    }

    private fun createSlot(stack: List<Int>, type: SlotType): Slot {
        var stackStr = ""
        stack.forEach { stackStr += it }
        return Slot(stackStr, getTransform(stack), type, 0.0)
    }

    fun render() = program.apply {
        drawer.isolated {
            fill = null
            stroke = foreground
            view *= fromBoard
            strokeWeight = lineThickness
            lineCap = LineCap.BUTT

            val finished = mutableListOf<Slot>()

            for (it in objects) {
                if (!it.developing && it.development < 0.01) {
                    finished.add(it)
                    continue
                }

                for (stack in deleteQueue) if (stack.length <= it.stack.length && stack == it.stack.substring(
                        0,
                        stack.length
                    )
                )
                    it.developing = false

                val t = if (it.developing) 1.0 else 0.0
                val y = it.development
                it.development += (t - y) * (1 - E.pow(-Slot.rate * deltaTime))
                renderSlot(it)
                renderedObjects++
            }
            objects.removeAll(finished)
            objects.addAll(addQueue)
            addQueue.clear()
            deleteQueue.clear()


            val mouse = this@GameRenderer.mouse
            var newHoveredMove: Int? = null
            for (i in 0 until blanks.size) {
                val blank = blanks[i]
                blank.targetOpacity = 0.0
                if (showMoves) blank.targetOpacity = Blank.passiveOpacity
                if (mouse != null) {
                    val transformed = blank.transform.inversed * mouse
                    if (Rectangle(0.0, 0.0, 1.0, 1.0).contains(transformed)) {
                        blank.targetOpacity = Blank.hoverOpacity
                        newHoveredMove = i
                    }
                }

                blank.opacity += (blank.targetOpacity - blank.opacity) * (1 - E.pow(-Slot.rate * deltaTime))
                renderBlank(blank)
            }

            hoveredMove = newHoveredMove
        }
    }

    private fun Drawer.renderSlot(slot: Slot) = isolated {
        view *= slot.transform
        if (!checkBounds()) return@isolated
        renderedObjects++
        stroke = stroke?.opacify((slot.development * 3).coerceAtMost(1.0))
        when (slot.type) {
            SlotType.BOARD -> {
                for (i in 0 until 2) {
                    val offset = (i + 1) * 1.0 / 3.0
                    lineSegment(0.5 - slot.development * 0.5, offset, 0.5 + slot.development * 0.5, offset)
                    lineSegment(offset, 0.5 - slot.development * 0.5, offset, 0.5 + slot.development * 0.5)
                }
            }
            SlotType.X -> {
                strokeWeight = Slot.thickness
                val len = (sqrt(2.0) - Slot.thickness) * slot.development * 0.5
                translate(0.5, 0.5)
                rotate(45 + (1 - slot.development) * 90)
                lineSegment(0.0, -len, 0.0, len)
                lineSegment(-len, 0.0, len, 0.0)
            }
            SlotType.O -> {
                strokeWeight = Slot.thickness
                circle(0.5, 0.5, slot.development * 0.5)
            }
        }
    }


    private fun Drawer.renderBlank(blank: Blank) {
        if (blank.opacity <= 0.01) return
        isolated {
            view *= blank.transform
            if (!checkBounds()) return@isolated
            renderedObjects++

            stroke = null
            fill = foreground.opacify(blank.opacity)
            roundedRectangle(0.0, 0.0, 1.0, 1.0, 0.2)
        }
    }

    companion object {
        var lineThickness = 0.015
        var padding = 0.2
    }
}

operator fun Matrix44.times(vec: Vector2): Vector2 = (this * Vector4(vec.x, vec.y, 0.0, 1.0)).xy
private val Symbol.State.rendered: SlotType
    get() = when (this) {
        Symbol.State.X -> SlotType.X
        Symbol.State.O -> SlotType.O
        Symbol.State.BLANK -> throw IllegalStateException("Rendered Symbol cannot be blank")
    }

fun Drawer.checkBounds(): Boolean {
    val corner = view * Vector2.ZERO
    val dimensions = view * Vector2.ONE - corner
    return Rectangle(corner, dimensions.x, dimensions.y).intersects(
        Rectangle(
            0.0,
            0.0,
            width.toDouble(),
            height.toDouble()
        )
    )
}

fun getTransform(stack: List<Int>, initial: Matrix44): Matrix44 {
    if (stack.isEmpty()) return initial
    val newTransform = initial * transform {
        val x = stack[0] % 3
        val y = stack[0] / 3
        translate((x + GameRenderer.padding * 0.5) * 1.0 / 3.0, (y + GameRenderer.padding * 0.5) * 1.0 / 3.0)
        scale(1.0 / 3.0 * (1 - GameRenderer.padding))
    }
    if (stack.size == 1) return newTransform
    return getTransform(stack.subList(1, stack.size), newTransform)
}

fun getTransform(vararg slot: Int): Matrix44 = getTransform(slot.map {it})
fun getTransform(stack: List<Int>): Matrix44 = getTransform(stack, Matrix44.IDENTITY)


