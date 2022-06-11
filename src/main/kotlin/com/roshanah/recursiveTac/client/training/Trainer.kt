//package com.roshanah.recursiveTac.client.training
//
//import com.roshanah.recursiveTac.client.GamePlayer
//import com.roshanah.recursiveTac.client.elements.Game
//import com.roshanah.recursiveTac.client.elements.Player
//import com.roshanah.recursiveTac.client.elements.Symbol
//import com.roshanah.recursiveTac.client.elements.getStack
//import com.roshanah.recursiveTac.ml.CompressedNetwork
//import com.roshanah.recursiveTac.ml.Network
//import org.openrndr.extra.noise.value
//import java.lang.Math.random
//import kotlin.math.pow
//
//class Trainer(val depth: Int, network: CompressedNetwork) {
//
//    var player = AmbiguousPlayer(GamePlayer(depth))
//        private set
//    var viewedGame: Game = player.original.player.game
//
//    val xInputs: List<() -> Double> = encodeInputs(Player.X)
//    val oInputs: List<() -> Double> = encodeInputs(Player.O)
//    var teacher: Network = network.extract(oInputs)
//    var student: Network = network.random.extract(xInputs)
//    var studentPlayer = Player.X
//        private set
//
//    var state = State.PLAY
//        private set
//
//    var batchSize = 50000
//    var descentStep = 0.005
//    val batch = mutableListOf<CompressedNetwork>()
//
//    val winLoss = mutableListOf<Boolean>()
//    var minWinRatio = 0.65
//
//    private data class CorrectionState(val game: Game, val moveSet: MutableSet<List<Int>>)
//    private val correctionStack: MutableList<CorrectionState> = mutableListOf()
//
//    fun update() {
//        when (state) {
//            State.PLAY -> {
//                if(!player.original.player.game.evaluate().finished) {
//                    if (player.original.player.player == studentPlayer)
//                        makeMove(student.fire())
//                    else
//                        makeMove(teacher.fire())
//                }
////                println(xInputs.map { it() })
//
//                val evaluation = player.original.player.game.evaluate()
//                if (evaluation.finished) {
//                    state = if (evaluation.player == studentPlayer) {
//                        if (correctionStack.isEmpty()) winLoss += true
//                        State.WIN
//                    } else {
//                        if (correctionStack.isEmpty()) {
//                            if (evaluation.player == studentPlayer.other()) winLoss += false
//                        }
//                        State.CORRECTION
//                    }
//                }
//
//            }
//            State.WIN -> {
//                descendGame()
//                player = AmbiguousPlayer(GamePlayer(depth))
//                student = student.compressed.extract(if(studentPlayer == Player.X) oInputs else xInputs)
//                teacher = teacher.compressed.extract(if(studentPlayer == Player.X) xInputs else oInputs)
//                studentPlayer = studentPlayer.other()
//                state = if (batch.size >= batchSize)
//                    State.DESCENT
//                else
//                    State.PLAY
//
//                correctionStack.clear()
//            }
//            State.CORRECTION -> {
//                val studentMoves = player.extractMoves(studentPlayer)
////                println(player.original.player.moveHistory.zip(player.original.player.history))
//                for (i in correctionStack.size until  studentMoves.size){
//                    correctionStack.add(CorrectionState(studentMoves[i].first, mutableSetOf(studentMoves[i].second)))
//                }
//
//
////                println(correctionStack.last().moveSet)
//
//                while (correctionStack.last().moveSet.size >= correctionStack.last().game.possibleMoves)
//                    correctionStack.removeLast()
//
//                if(correctionStack.isEmpty()) error("perfect play acheived")
//
////                  Undo the game so the student gets another chance to make a move
//                player.original.player.currentIndex = (correctionStack.size - 1) * 2 + if(studentPlayer == Player.X) 0 else 1
//                player = AmbiguousPlayer(player.original.player.fork())
////                println(player.history.size)
//
//                val last = correctionStack.last()
//                viewedGame = last.game
//                val moveChoices = buildList {
//                    student.fire().forEachIndexed { i, value ->
//                        val stack = getStack(i)
//                        if (viewedGame.getSlot(stack).active)
//                            add(Pair(stack, value))
//                    }
//                }.sortedByDescending { it.second }
//
//                for(move in moveChoices){
//                    if(!last.moveSet.contains(move.first)) {
//                        player.makeMove(move.first)
//                        last.moveSet.add(move.first)
//                        state = State.PLAY
//                        break
//                    }
//                }
//
//                viewedGame = player.normalized
//
//            }
//            State.DESCENT -> {
//                var avg = student.zero
//                batch.forEach { avg += it }
//                avg /= batch.size
//
////                println(avg.formattedString())
//
//                state = if (winLoss.sumOf { if (it) 1.0 else 0.0 } / winLoss.size >= minWinRatio) {
//                    State.REPLACEMENT
//                } else {
//                    State.PLAY
//                }
//
//                student = student.descend(avg, descentStep, if (studentPlayer == Player.X) oInputs else xInputs)
//                teacher = teacher.compressed.extract(if (studentPlayer == Player.X) xInputs else oInputs)
//                studentPlayer = studentPlayer.other()
//                winLoss.clear()
//                batch.clear()
//            }
//            State.REPLACEMENT -> {
//                teacher = student.compressed.extract(if (studentPlayer == Player.X) oInputs else xInputs)
//                student = teacher.compressed.random.extract(if (studentPlayer == Player.X) xInputs else oInputs)
//                state = State.PLAY
//            }
//        }
//    }
//
//    private fun descendGame() {
//        for (move in player.extractMoves(studentPlayer)) {
//            viewedGame = move.first
//            student.fire()
//            val grad = student.computeGradient(encodeKey(viewedGame, move.second))
//            batch += grad
//        }
//    }
//
//    constructor(depth: Int, layers: Int, nodes: Int) : this(
//        depth,
//        CompressedNetwork.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, layers, nodes, 9.0.pow(depth + 1).toInt())
//    )
//
//    private fun makeMove(outputs: List<Double>) {
//        var max = 0.0
//        val filtered = buildList {
//            outputs.forEachIndexed { i, value ->
//                val stack = getStack(i)
//                if (player.normalized.getSlot(stack).active) {
//                    add(Pair(stack, value))
//                    if(value > max) max = value
//                }
//            }
//        }.map { if(max == 0.0) it else Pair(it.first, (it.second / max).pow(10.0)) }.sortedByDescending { it.second }
//
////        println(filtered)
//
//        val total = filtered.sumOf { it.second }
//        if (total == 0.0) {
//            player.makeMove(filtered.random().first)
//        } else {
//            var rand = random() * total
//            for ((move, probability) in filtered) {
//                if (rand < probability) {
//                    player.makeMove(move)
//                    break
//                }
//                rand -= probability
//            }
//        }
//
//        viewedGame = player.normalized
//    }
//
//    //    One hot encoding
//    private fun encodeInputs(player: Player) = buildList {
//        val slotStack = mutableListOf(0)
//        while (true) {
//            val thisStack = slotStack.toList()
//            add {
//                val slot = viewedGame[thisStack]
//                if (slot is Symbol && (slot.state == player.symbolState)) // self
//                    1.0
//                else
//                    0.0
//            }
//
//            add {
//                val slot = viewedGame[thisStack]
//                if (slot is Symbol && (slot.state == player.other().symbolState)) // opponent
//                    1.0
//                else
//                    0.0
//            }
//
//            if (slotStack.size < this@Trainer.player.original.player.depth + 1) {
//                slotStack.add(0)
//            } else {
//                while (slotStack.lastOrNull() == 8) slotStack.removeLast()
//                if (slotStack.isEmpty()) break
//                slotStack[slotStack.size - 1]++
//            }
//        }
//    }
//
//    private fun encodeKey(game: Game, slot: List<Int>): List<Double?> {
//        val currentSlot = mutableListOf<Int>()
//        val out = mutableListOf<Double?>()
//
//        do {
//            while (currentSlot.size < game.depth + 1) currentSlot += 0
//            val it = game[currentSlot]
//            out += when {
//                currentSlot.containsAll(slot) -> 1.0
//                it.active -> 0.0
//                else -> null
//            }
//            currentSlot[currentSlot.size - 1]++
//            while (currentSlot.last() == 9) {
//                currentSlot.removeLast()
//                if (currentSlot.isEmpty()) break
//                currentSlot[currentSlot.size - 1]++
//            }
//        } while (currentSlot.isNotEmpty())
//
//        return out
//    }
//
//    enum class State {
//        PLAY, CORRECTION, WIN, DESCENT, REPLACEMENT
//    }
//
//}