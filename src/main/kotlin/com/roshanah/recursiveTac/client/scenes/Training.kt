//package com.roshanah.recursiveTac.client.scenes
//
//import com.roshanah.recursiveTac.client.GamePlayer
//import com.roshanah.recursiveTac.client.cameraTransform
//import com.roshanah.recursiveTac.client.elements.Game
//import com.roshanah.recursiveTac.client.elements.getStack
//import com.roshanah.recursiveTac.client.foreground
//import com.roshanah.recursiveTac.client.rendering.GameRenderer
//import com.roshanah.recursiveTac.client.rendering.getTransform
//import com.roshanah.recursiveTac.client.rendering.player
//import com.roshanah.recursiveTac.client.rendering.times
//import com.roshanah.recursiveTac.client.sizeTransform
//import com.roshanah.recursiveTac.client.training.AmbiguousPlayer
//import com.roshanah.recursiveTac.client.training.Trainer
//import kotlinx.coroutines.runBlocking
//import org.openrndr.*
//import org.openrndr.draw.isolated
//import org.openrndr.extra.shapes.roundedRectangle
//import org.openrndr.math.Matrix44
//import kotlin.math.pow
//
//class Training(program: Program, transform: Matrix44, depth: Int) : Scene(program, transform) {
////    val trainer = Trainer(depth, 4, 9.0.pow(depth + 1).toInt() * 2)
//    val trainer = Trainer(depth, 2, 21)
//    val renderer = GameRenderer(0.0, 0.0, 1.0, program)
//
//    init {
//
//        program.apply {
//            keyboard.keyDown.listen {
//                when (it.key) {
//                    KEY_ARROW_RIGHT -> {
//                        update()
//                    }
//                }
//
//                when (it.name) {
//                    "n" -> {
//                        println(trainer.student.compressed.formattedString())
//                    }
//                    "t" -> {
//                        println(trainer.teacher.compressed.formattedString())
//                    }
//                    "b" -> {
//                        println(trainer.batch.size)
//                    }
//                    "r" -> {
//                        while (trainer.state != Trainer.State.REPLACEMENT)
//                            update()
//                    }
//                }
//            }
//        }
//
//        renderer.game = trainer.player.original.player.game
//
//
//    }
//
//    var slotChoices: List<Pair<List<Int>, Double>> = mutableListOf()
//
//    private fun update() {
//        trainer.update()
//        slotChoices = emptyList()
//        if (trainer.player.original.player.player == trainer.studentPlayer) {
//            slotChoices = buildList {
//                trainer.student.fire().forEachIndexed { i, value ->
//                    val stack = getStack(i)
//                    if (trainer.player.normalized.getSlot(stack).active)
//                        add(Pair(trainer.player.normal.inverse(stack), value))
//                }
//            }
//            val max = slotChoices.maxByOrNull { it.second }?.second
//            if (max != null && max > 0.0) {
//                slotChoices = slotChoices.map { Pair(it.first, (it.second / max).pow(6)) }
//            }
//        }
//        when (trainer.state){
//            Trainer.State.REPLACEMENT -> println(trainer.teacher.compressed.formattedString())
//            Trainer.State.DESCENT -> println("win ratio: ${trainer.winLoss.sumOf { if (it) 1.0 else 0.0 } / trainer.winLoss.size}")
//        }
//    }
//
//    private fun train(hertz: Double){
//        val start = System.nanoTime() * 1e-9
//        while(System.nanoTime() * 1e-9 - start < 1 / hertz)
//            update()
//    }
//
//    var trained = false
//
//    override fun Program.renderBody() {
//
//        renderer.game = trainer.player.original.player.game
//        renderer.render()
//        if (keyboard.pressedKeys.contains("left-shift") && keyboard.pressedKeys.contains("arrow-right")) {
//            train(60.0)
//        }
//
////        if(slotChoices.isNotEmpty()) println(slotChoices.map { it.second })
//
//        for (move in slotChoices) drawer.isolated {
//            view *= renderer.fromBoard * getTransform(move.first)
//            fill = foreground.opacify(move.second)
//            stroke = null
//            roundedRectangle(0.0, 0.0, 1.0, 1.0, 0.2)
//        }
//
//
////        println(keyboard.pressedKeys)
//    }
//
//
//}