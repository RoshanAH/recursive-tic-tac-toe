package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.*
import com.roshanah.rt3.client.elements.Game
import com.roshanah.rt3.client.rendering.*
import com.roshanah.rt3.client.rendering.GameRenderer.Companion.padding
import org.openrndr.*
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.E
import kotlin.math.pow

open class Scene(val program: Program, val transform: Matrix44) {
    val active get() = scenes.contains(this)
    val buttons = mutableListOf<Button>()
    val textFields = mutableListOf<TextField>()
    protected open fun Program.renderBody() {}

    init {
        program.apply {
            mouse.buttonDown.listen {
                if (!active) return@listen
                if (it.button != MouseButton.LEFT) return@listen
                for (button in buttons) {
                    if (button.targetOpacity == 0.0) continue
                    val localPos = (sizeTransform * cameraTransform * button.transform).inversed * it.position
                    if (this@Scene.bounds.contains(localPos))
                        button.onClick()
                }

                for (field in textFields) {
                    if (field.targetOpacity == 0.0) continue
                    val localPos = (sizeTransform * cameraTransform * field.transform).inversed * it.position
                    if (field.rect.contains(localPos)) {
                        textFields.forEach { it.focused = false }
                        field.focused = true
                    }
                }
            }

            keyboard.character.listen { event ->
                if(!active) return@listen
                textFields.forEach { field ->
                    if (field.focused) {
                        field.text += event.character
//                        println(field.text)
                    }
                }
            }

            keyboard.keyDown.listen { event ->
                if(!active) return@listen
                when (event.key) {
                    KEY_BACKSPACE -> textFields.forEach {
                        if (it.focused && it.text.isNotEmpty())
                            it.text = it.text.substring(0, it.text.length - 1)
                    }
                    KEY_ENTER -> textFields.forEach {
                        if (it.focused) it.onEnter()
                    }
                }
            }
        }

    }

    private val bounds = Rectangle(0.0, 0.0, 1.0, 1.0)

    fun render() {

        program.drawer.isolated {
            view *= transform
            program.renderBody()
        }

        program.drawer.isolated {

            for (button in buttons) isolated loop@{
                view *= button.transform
                button.opacity += (button.targetOpacity - button.opacity) * (1 - E.pow(-6.0 * program.deltaTime))
                if (!checkBounds() || button.opacity < 0.01) return@loop
                renderedObjects++
                stroke = null
                fill = foreground.opacify(button.opacity)

                val localPos = (sizeTransform * cameraTransform * button.transform).inversed * program.mouse.position

                val targetHover =
                    if (this@Scene.bounds.contains(localPos) && button.sprite.bounds.contains(localPos))
                        1.0
                    else 0.0

                button.hoverEffect += (targetHover - button.hoverEffect) * (1 - E.pow(-6.0 * program.deltaTime))

                for (shape in button.sprite) {
                    shape(shape.transform(transform {
                        val center = shape.bounds.center
                        val offset = center - Vector2(0.5, 0.5)
                        translate(offset * button.hoverEffect * Button.hoverDisplacement)
                    }))
                }
            }

            for (field in textFields) isolated loop@{
                view *= field.transform
                field.opacity += (field.targetOpacity - field.opacity) * (1 - E.pow(-6.0 * program.deltaTime))
                if (!checkBounds() || field.opacity < 0.01) return@loop
                renderedObjects++
                stroke = foreground.opacify(field.opacity)
                fill = background.opacify(field.opacity)
                strokeWeight = GameRenderer.lineThickness
                rectangle(field.rect)
                if (field.text.isEmpty()) return@loop
                stroke = null
                fill = foreground.opacify(field.opacity)

                isolated {
                    fontMap = font

                    var width = 0.0
                    var clippedText = ""
                    for (char in field.text) {
                        width += font.characterWidth(char) / font.size
                        if (width <= field.rect.width)
                            clippedText += char
                        else
                            break
                    }

                    field.text = clippedText

                    view *= transform {
                        val width = font.width(field.text)
                        scale((1 - padding * 0.5) / font.size)
                        translate(-width * 0.5 + font.characterWidth(field.text[0]) * 1.2, font.height * 1.5)
                    }
                    text(field.text)
                }
            }
        }
    }

    fun attachInput(renderer: GameRenderer, onMove: (Int) -> Unit) {
        val canvasToBoard: (Vector2) -> Vector2 = {
            (sizeTransform * cameraTransform * transform * renderer.fromBoard).inversed * it
        }
        program.apply {
            mouse.moved.listen {
                if (active) renderer.mouse = canvasToBoard(it.position)
            }

            mouse.buttonDown.listen {
                if (active) {
                    when (it.button) {
                        MouseButton.LEFT -> {
                            val move: Int = renderer.hoveredMove ?: return@listen
                            onMove(move)
                        }
                    }
                }
            }

            keyboard.keyDown.listen {
                if (active) {
                    when (it.key) {
                        KEY_SPACEBAR -> renderer.showMoves = true
                    }
                }
            }

            keyboard.keyUp.listen {
                if (active) {
                    when (it.key) {
                        KEY_SPACEBAR -> renderer.showMoves = false
                    }
                }
            }
        }

    }

    fun attachSaving(player: GamePlayer, onSave: (String) -> Unit) {
        program.keyboard.keyDown.listen {
            if (active && it.name == "s" && it.modifiers.contains(KeyModifier.CTRL)) onSave(player.serialize())
        }
    }
}

fun FontImageMap.width(text: String): Double = text.sumOf { characterWidth(it) }

class MenuBuilder(val program: Program, val transform: Matrix44) {
    val board = GameRenderer(0.0, 0.0, 1.0, program)
    var depth = 0
    val scene: Scene = object : Scene(program, transform) {

        init {
            board.game = Game(depth)
        }

        override fun Program.renderBody() {
            board.render()
        }
    }
    var onEnter: () -> Unit = {}

    fun button(slot: List<Int>, sprite: List<Shape>, builder: MenuBuilder.() -> Unit) : Button{
        if (slot.size - 1 > depth) depth = slot.size - 1
        val buttonTransform = transform * board.fromBoard * getTransform(slot)
        val sceneBuilder = MenuBuilder(program, buttonTransform)
        sceneBuilder.builder()

        val button = Button(
                buttonTransform,
                sprite,
                1.0,
                0.0,
                sceneBuilder.scene
            )
        scene.buttons.add(button)
        return button
    }

    fun button(slot: List<Int>, sprite: List<Shape>, clickScene: Scene) {
        if (slot.size - 1 > depth) depth = slot.size - 1
        val buttonTransform = transform * board.fromBoard * getTransform(slot)

        scene.buttons.add(
            Button(
                buttonTransform,
                sprite,
                1.0,
                0.0,
                clickScene
            )
        )
    }

    fun textFieldToMenu(row: Int, builder: (MenuBuilder).(String) -> Unit) {
        val boardTransform = transform * board.fromBoard
        val slotTransform = getTransform(1 + row * 3)
        val fieldTransform = boardTransform * slotTransform

        val width = (getTransform(0).inversed * slotTransform * Vector2.ONE -
                getTransform(2).inversed * slotTransform * Vector2.ZERO).x

        lateinit var field: TextField
        field = TextField(fieldTransform, width) {
            val menuBuilder = MenuBuilder(program, fieldTransform)
            menuBuilder.builder(field.text)
            menuBuilder.scene
        }
        scene.textFields.add(field)
    }

    fun textFieldToScene(row: Int, builder: TextField.(String) -> Scene) {
        val boardTransform = transform * board.fromBoard
        val slotTransform = getTransform(1 + row * 3)
        val fieldTransform = boardTransform * slotTransform

        val width = (getTransform(0).inversed * slotTransform * Vector2.ONE -
                getTransform(2).inversed * slotTransform * Vector2.ZERO).x

        lateinit var field: TextField
        field = TextField(fieldTransform, width) { field.builder(it) }

        this.scene.textFields.add(field)
    }

    fun button(slot: Int, sprite: List<Shape>, builder: MenuBuilder.() -> Unit) = button(listOf(slot), sprite, builder)
    fun button(slot: Int, sprite: List<Shape>, builder: MenuBuilder.() -> Unit, onClick: Button.() -> Unit): Button{
        val button = button(listOf(slot), sprite, builder)
        val old = button.onClick
        button.onClick = {
            old()
            onClick(button)
        }
        return button
    }
    fun button(slot: Int, sprite: List<Shape>, clickScene: Scene) = button(listOf(slot), sprite, clickScene)
}

fun buildMenu(program: Program, transform: Matrix44, builder: MenuBuilder.() -> Unit): Scene {
    val out = MenuBuilder(program, transform)
    out.builder()
    return out.scene
}