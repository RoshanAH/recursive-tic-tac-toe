package com.roshanah.rt3.client.training

import com.roshanah.rt3.client.GamePlayer
import com.roshanah.rt3.client.elements.*

import com.roshanah.rt3.ml.List1d

import com.roshanah.rt3.ml.Network
import kotlinx.coroutines.yield
import kotlin.math.pow
import kotlin.random.Random


class Trainer(val depth: Int, private val initialNetwork: Network) {

    private var stopped = false
    private var stepTerm: String? = null
    private var yieldHertz: Double? = null
    private var lastYield: Long = System.nanoTime()
    private var statusListener = mutableSetOf<Pair<String, () -> Unit>>()
    private var query: String? = null
    fun listen(status: String, action: () -> Unit) {
        statusListener += Pair(status, action)
    }

    var state = TrainerState(Game(depth), "initialized")
        private set

    var learningRate = 0.1
    var minCost = 0.1
    var minWinRatio = 0.75
    var gameSampleSize = 1000
        set(value) {
            field = value + if (value % 2 == 1) 1 else 0
        }


    constructor(depth: Int, layers: Int, nodes: Int) : this(
        depth,
        Network.random((1..depth + 1).sumOf { 9.0.pow(it).toInt() } * 2, layers, nodes, 9.0.pow(depth + 1).toInt())
    )


    suspend fun train() {
        var teacher: Network = initialNetwork
        var student: Network = initialNetwork.random
        var totalGamesPlayed = 0

        fun Game.State.score(player: Player) = when (this.player) {
            player -> 1.0
            !player -> 0.0
            else -> 0.5
        }

        while (true) {
            var blank = Game(depth)
            val moveSet = mutableMapOf<List1d, CompoundKey>()
            val fields: MutableMap<String, Any>.() -> Unit = {
                this["games"] = totalGamesPlayed
                this["moves"] = moveSet.size
                this["student"] = student
                this["teacher"] = teacher
            }
            var winRatio = 0.0
            while (true) {
                for (i in 0 until gameSampleSize step 2) {
                    var moves = MoveSearch(setOf(), setOf())
                    for (player in Player.values()) {
                        winRatio += playGame(student, teacher, depth).score(player)
                        moves += findMoves(blank, player, student, teacher)
                        totalGamesPlayed++
                        pass(blank, "played ${player.name} game"){
                            fields()
                            this["i"] = "game: $i of $gameSampleSize"
                        }
                    }

                    for (move in moves.merge) {
                        moveSet.putIfAbsent(move.encoded, mutableListOf(move.key))
                        moveSet[move.encoded]?.add(move.key)
                    }

                }

                winRatio /= gameSampleSize
                pass(blank, "finished playing games") {
                    fields()
                    this["winRatio"] = winRatio
                }
                if (winRatio >= minWinRatio) break

                var i = 0
                while (true) {
                    var sum = student.zero
                    var cost = 0.0
                    for ((input, key) in moveSet) {
                        val fired = student.fire(input)
                        val avg = key.avg
                        cost += fired.last().zip(avg).sumOf { move -> move.second?.let { (move.first - it).pow(2) } ?: 0.0}
                        val gradient = student.computeGradient(fired, avg)
//                        println(gradient)
                        sum += gradient
                    }

                    cost /= moveSet.size
                    student = student.descend(sum / moveSet.size, learningRate)
                    if (cost < minCost) break
                    pass(blank, "training") {
                        fields()
                        this["cost"] = cost
                        this["i"] = "training iteration $i"
                    }
                    i++
                }
            }
            teacher = student
            student = student.random
            pass(blank, "replaced", fields)
        }
    }

    //    private suspend fun findMoves(
//        game: Game,
//        player: Player,
//        student: Network,
//        teachers: List<Network>,
//    ): MoveSearch {
//        val teacher = teachers.last()
//        val moveChoices = student.getChoices(game, player)
//        val drawMoves = mutableSetOf<Move>()
//
//        for (move in moveChoices) {
////            exclude move if for any teacher there are no wins
//            if (teachers.any { it != teacher && findMoves(game, player, student, it).winning.isEmpty() }) continue
//            val moved = game(move.slot, player)
//            pass(moved, "playing: student moved")
//            val eval = moved.evaluate()
//            if (eval.player == player) return MoveSearch(setOf(move), setOf()) // check if the student won
//            val teacherMoved = moved(teacher.calculateMove(moved, player.other()).slot, player.other())
//            pass(teacherMoved, "playing: teacher moved")
//            if (teacherMoved.evaluate() == Game.State.TIE) {
//                drawMoves += move
//                continue
//            }
////            we do not need to check for win here because a teacher move will not produce a win
//
//            val search = findMoves(teacherMoved, player, student, teacher)
//            if (search.winning.isNotEmpty()) return MoveSearch(
//                search.winning,
//                drawMoves + search.drawing
//            ) // short circuit return here because we only want to find a win
//            drawMoves += search.drawing // keep track of draws in case a win is not possible
//        }
//
//        return MoveSearch(setOf(), drawMoves)
//
//    }

    private suspend fun findMoves(
        game: Game,
        player: Player,
        student: Network,
        teacher: Network,
        studentSeed: Long = student.makeSeed,
        teacherSeed: Long = teacher.makeSeed
    ): MoveSearch {
        val moveChoices = student.getChoices(game, player).toMutableList()
        val drawMoves = mutableSetOf<Move>()

        while (moveChoices.isNotEmpty()) {
            val movePair = moveChoices.randomBy(Random(studentSeed)) { it.second }
            moveChoices.remove(movePair)
            val move = movePair.first

            val moved = game(move.slot, player)
            pass(moved, "playing: student moved")
            val eval = moved.evaluate()
            if (eval.player == player) return MoveSearch(setOf(move), setOf()) // check if the student won
            if (eval == Game.State.TIE) {
                drawMoves += move
                continue
            }
            val teacherMoved = moved(teacher.calculateMove(moved, player.other(), teacherSeed).slot, player.other())
            pass(teacherMoved, "playing: teacher moved")
            if (teacherMoved.evaluate() == Game.State.TIE) {
                drawMoves += move
                continue
            }
//            we do not need to check for win here because a teacher move will not produce a win

            val search = findMoves(teacherMoved, player, student, teacher, studentSeed, teacherSeed)
            if (search.winning.isNotEmpty()) return MoveSearch(
                search.winning,
                drawMoves + search.drawing
            ) // short circuit return here because we only want to find a win
            drawMoves += search.drawing // keep track of draws in case a win is not possible
        }

        return MoveSearch(setOf(), drawMoves)
    }

    suspend fun playGame(x: Network, o: Network, depth: Int): Game.State {
        var game = Game(depth)
        var state = Game.State.ONGOING

        while (state.finished) {
            game = game(x.calculateMove(game, Player.X).slot, Player.X)
            state = game.evaluate()
            pass(game, "played in test game")
            if (state.finished) break
            game = game(o.calculateMove(game, Player.O).slot, Player.O)
            pass(game, "played in test game")
        }

        return state
    }


    private suspend fun pass(state: TrainerState = this.state) {
        val time = System.nanoTime()
        this.state = state

        statusListener.forEach {
            if (state.status.contains(it.first)) {
                it.second()
            }
        }

        if (1.0 / ((time - lastYield) * 1e-9) < (yieldHertz ?: 0.0)) {
            yield()
            lastYield = time
        }
        while (stopped) {
            yield()
            lastYield = time
        }
        stepTerm?.let {
            if (state.status.contains(it)) {
                stopped = true
                stepTerm = null
            }
        }
        query?.let {
            if(state.fields[it] != null) {
                stopped = true
                query = null
            }
        }
    }

    private suspend fun pass(game: Game, status: String, fields: MutableMap<String, Any>.() -> Unit) {
        val map = mutableMapOf<String, Any>()
        map.fields()
        pass(TrainerState(game, status, map.toMap()))
    }

    private suspend fun pass(game: Game, status: String) = pass(TrainerState(game, status))

    fun play(yieldHertz: Double? = null) {
        stopped = false
        stepTerm = null
        this.yieldHertz = yieldHertz
    }

    fun pause() {
        stopped = true
    }

    fun step() = step("")

    fun step(status: String) {
        stopped = false
        stepTerm = status
    }

    suspend fun query(field: String): Any {
        stopped = false
        query = field
        while(!stopped) yield()
        return state.fields[query]!!
    }
}

private typealias CompoundKey = MutableList<List<Double?>>

private val CompoundKey.avg: List<Double?>
    get() {
        operator fun List<Double?>.plus(other: List<Double?>) =
            zip(other).map { it.second?.let { it1 -> it.first?.plus(it1) } }

        var total = first().map<Double?, Double?> { 0.0 }
        forEach { total = total + it }
        return total.map { it?.let { it / size.toDouble() } }
    }


data class MoveSearch(val winning: Set<Move>, val drawing: Set<Move>) {
    operator fun plus(other: MoveSearch) = MoveSearch(winning + other.winning, drawing + other.drawing)
    val merge: Set<Move>
        get() = buildSet {
            addAll(winning)
            val subtraction = drawing.toMutableSet()
            winning.forEach { winMove ->
                subtraction.removeAll { drawMove ->
                    winMove.encoded == drawMove.encoded
                }
            }
            addAll(subtraction)
        }
}

class TrainerState(val game: Game, val status: String, val fields: Map<String, Any> = mapOf())

fun <E> Collection<E>.randomBy(random: Random = Random.Default, predicate: (E) -> Double): E {
    val total = sumOf(predicate)
    if (total <= 0.0) return random(random)
    var rand = random.nextDouble() * total
    for (e in this) {
        rand -= predicate(e)
        if (rand <= 0.0) return e
    }

    return last()
}


fun Game.encodeKey(slot: SlotIndex): List<Double?> {
    val out = mutableListOf<Double?>()

    foreachIndexed { i, it ->
        if (i.stack.size != depth + 1) return@foreachIndexed // iterate only on symbols
        out += when {
            i == slot -> 1.0
            it.active -> 0.0
            else -> null
        }
    }

    return out
}


operator fun Game.invoke(move: SlotIndex, player: Player) = this(positionToMove(move), player)


val Network.makeSeed: Long get() = Random.Default.nextLong(Long.MAX_VALUE / (3.0.pow(inputs / 2 + 1).toLong() - 1))
val List1d.encodeInput: Long
    get() {
        var total: Long = 0
        for (i in indices step 2) {
            total +=
                (if (this[i] == 0.0 && this[i + 1] == 0.0) 0
                else if (this[i] == 0.0) 1
                else 2) * 3.0.pow(i / 2).toLong()
        }

        return total
    }

fun Network.calculateMove(game: Game, player: Player, seed: Long = makeSeed): Move {
    val (normal, transform) = game.normal
    val inputs = normal.rawEncode(player)
    val moveRandom = Random(seed * inputs.encodeInput)
    val outputs = fire(inputs).last()

    val filtered = buildList {
        outputs.forEachIndexed { i, it ->
            val stack = i.slotIndex(game.depth)
            if (normal[stack].active) {
                add(Pair(stack, it.coerceAtLeast(0.0)))
            }
        }
    }

    val normalSlot = filtered.randomBy(moveRandom) { it.second }.first
    val slot = transform.inverse(normalSlot)

    return Move(game, normal, inputs, slot, normalSlot)
}

fun Network.getChoices(game: Game, player: Player): List<Pair<Move, Double>> {
    val (normal, transform) = game.normal
    val inputs = normal.rawEncode(player)
    val outputs = fire(inputs).last()

    val filtered = buildList {
        outputs.forEachIndexed { i, it ->
            val stack = i.slotIndex(game.depth)
            if (normal[stack].active) {
                add(Pair(stack, it.coerceAtLeast(0.0)))
            }
        }
    }.sortedByDescending { it.second }

    return filtered.map { Pair(Move(game, normal, inputs, transform.inverse(it.first), it.first), it.second) }
}

data class Move(
    val originalGame: Game,
    val normalizedGame: Game,
    val encoded: List1d,
    val slot: SlotIndex,
    val normalSlot: SlotIndex
) {
    override operator fun equals(other: Any?): Boolean = other is Move && encoded == other.encoded && slot == other.slot
    val key get() = normalizedGame.encodeKey(normalSlot)
}


fun moveFromOriginal(player: GamePlayer, slot: SlotIndex) = moveFromOriginal(player.game, player.player, slot)
fun moveFromOriginal(originalGame: Game, player: Player, slot: SlotIndex): Move {
    val (normalized, transform) = originalGame.normal

    val inputs = buildList {
        normalized.foreach {
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

    return Move(originalGame, normalized, inputs, slot, transform(slot))
}
