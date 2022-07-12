package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.foreground
import com.roshanah.rt3.client.rendering.GameRenderer
import com.roshanah.rt3.client.rendering.getTransform
import com.roshanah.rt3.client.training.Trainer
import com.roshanah.rt3.client.training.encode

import com.roshanah.rt3.ml.Network
import org.openrndr.*
import org.openrndr.draw.isolated
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.math.Matrix44

class Training(program: Program, transform: Matrix44, depth: Int) : Scene(program, transform) {
    val trainer = Trainer(depth)
    val renderer = GameRenderer(0.0, 0.0, 1.0, program)

    init {

        program.apply {
            keyboard.keyDown.listen {
                when (it.key) {
                    KEY_ARROW_RIGHT -> {
                        update()
                    }
                }

                when (it.name) {
                    "n" -> {
                        if(it.modifiers.contains(KeyModifier.CTRL))
                            println(trainer.student.serialized)
                        else
                            println(trainer.student)
                    }
                    "t" -> {
                        if(it.modifiers.contains(KeyModifier.CTRL))
                            println(trainer.teacher.serialized)
                        else
                            println(trainer.teacher)
                    }
                    "b" -> {
                        println(trainer.moveSet.size)
                    }
                    "r" -> {
                        while (trainer.state != Trainer.State.REPLACEMENT)
                            update()
                    }
                    "s" -> {
                        println(trainer.state)
                    }
                    "p" -> {
                        println(trainer.studentPlayer)
                    }
                    "g" -> {
                        println(trainer.totalGamesPlayed)
                    }
                    "w" -> {
                        if(it.modifiers.contains(KeyModifier.CTRL))
                            println(trainer.winRatio)
                        else
                            println(trainer.winRatio.sum() / trainer.winRatio.size)
                    }
                    "m" -> {
                        if(it.modifiers.contains(KeyModifier.CTRL))
//                            println(trainer.winRatio)
                        else
                            println(trainer.moveSet.size)
                    }
                }
            }
        }

        renderer.game = trainer.player.game

    }

    var slotChoices: List<Pair<List<Int>, Double>> = mutableListOf()

    private fun update() {
        trainer.update()
//        slotChoices = emptyList()
//        if (trainer.player.original.player.player == trainer.studentPlayer) {
//            slotChoices = buildList {
//                trainer.apply {
//                    student.fire(player.normalized.encode(studentPlayer)).last().forEachIndexed { i, value ->
//                        val stack = getStack(i)
//                        if (trainer.player.normalized.getSlot(stack).active)
//                            add(Pair(player.normal.inverse(stack), value))
//                    }
//                }
//            }
//            val max = slotChoices.maxByOrNull { it.second }?.second
//            if (max != null && max > 0.0) {
//                slotChoices = slotChoices.map { Pair(it.first, it.second / max) }
//            }
//        }
        when (trainer.state){
           Trainer.State.REPLACEMENT -> println("Replaced. Played ${trainer.totalGamesPlayed} games")
//            Trainer.State.REPLACEMENT -> println(trainer.teacher)
//            Trainer.State.DESCENT -> println("win ratio: ${trainer.winRatio.sumOf { if (it) 1.0 else 0.0 } / trainer.winRatio.size}")
        }
    }



    private fun train(hertz: Double){
        val start = System.nanoTime() * 1e-9
        while(System.nanoTime() * 1e-9 - start < 1 / hertz)
            update()
    }

    override fun Program.renderBody() {

        renderer.game = trainer.player.game
        renderer.render()
        if (keyboard.pressedKeys.contains("left-shift") && keyboard.pressedKeys.contains("arrow-right")) {
            train(60.0)
        }

//        if(slotChoices.isNotEmpty()) println(slotChoices.map { it.second })

        for (move in slotChoices) drawer.isolated {
            view *= renderer.fromBoard * getTransform(move.first)
            fill = foreground.opacify(move.second)
            stroke = null
            roundedRectangle(0.0, 0.0, 1.0, 1.0, 0.2)
        }


//        println(keyboard.pressedKeys)
    }


}