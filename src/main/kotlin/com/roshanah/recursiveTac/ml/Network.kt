package com.roshanah.recursiveTac.ml

import java.math.MathContext
import kotlin.math.sqrt

typealias List1d = List<Double>
typealias List2d = List<List1d>
typealias List3d = List<List2d>

class Network {
    val network: List3d
    val inputs: Int
    val layers: Int
    val nodes: Int
    val outputs: Int

    constructor (network: List3d) {
        require(network.size >= 2) {
            "Network format error: network must have at least an input and output layer"
        }

        require(network.all { it.isNotEmpty() }) {
            "Network format error: each layer must have at least one node"
        }

//        Checking the number of the first node's weights excluding the bias
        inputs = network[0][0].size - 1
        require(inputs >= 1) {
            "Network format error: input size $inputs must be at least 1"
        }
//        All the layers excluding the output layers
        layers = network.size - 1
//        Checking the number of the first output node's weights excluding the bias
        nodes = if (layers == 0) 0 else network.last()[0].size - 1
//        Checking the length of the last layer
        outputs = network.last().size
        require(outputs >= 1) {
            "Network format error: output size $outputs must be at least 1"
        }


        for (j in network[0].indices) {
            val node = network[0][j]
            require(node.size - 1 == inputs) {
                "Network format error: input size ${node.size - 1} does not match with $inputs"
            }
        }

        for (i in 1 until network.size) {
            for (j in 1 until network[i].size) {
                val node = network[i][j]
                require(node.size - 1 == nodes) {
                    "Network format error: node size ${node.size - 1} does not match with $nodes"
                }
            }
        }
        this.network = network
    }

    private constructor(network: List3d, inputs: Int, layers: Int, nodes: Int, outputs: Int){
        this.network = network
        this.inputs = inputs
        this.layers = layers
        this.nodes = nodes
        this.outputs = outputs
    }

    fun fire(inputs: List1d): List2d = buildList {
        require(inputs.size == this@Network.inputs) {
            "Input size ${inputs.size} does not match network input size ${this@Network.inputs}"
        }
        add(inputs.map { it.coerceAtLeast(0.0) })
        for (i in network.indices) {
            add(network[i] * (this[i] + 1.0).map { it.coerceAtLeast(0.0) })
        }
    }

    fun computeGradient(fired: List2d, key: List1d): Network {
        require(fired.size == layers + 2 && fired[0].size == inputs && fired.last().size == outputs) {
            "Fired network does not fit the dimensions of this network"
        }
        require(key.size == outputs) {
            "Key size of ${key.size} does not match network output size of $outputs"
        }

        var activationDerivatives: List1d = fired.last().mapIndexed { i, it ->
            2 * (it - key[i])
        }

        val out = mutableListOf<List2d>()

        for (l in layers downTo 0) {
            val layer = network[l]
            val gradientLayer = mutableListOf<List1d>()
            val nextActivationDerivatives = (0 until layer[0].size - 1).map { 0.0 }.toMutableList()
            for (n in layer.indices) {
                val gradientNode = mutableListOf<Double>()
                val node = layer[n]
                for (w in 0 until node.size - 1) {
                    val activation = fired[l][w] // gives us previous layer because fired includes inputs values
                    val reluDerivative = if (activation > 0) 1.0 else 0.01 // leaky relu
                    gradientNode.add(reluDerivative * activation * activationDerivatives[n]) // chain rule
                    nextActivationDerivatives[w] += reluDerivative * node[w] * activationDerivatives[n]
                }
//                nextActivationDerivatives[node.lastIndex] += node.last() * activationDerivatives[n] makes no sense to keep track of bias activation because we cannot change it
                gradientNode.add(activationDerivatives[n]) // previous activation is always one since this is the bias node
                gradientLayer.add(gradientNode.toList())
            }
            out.add(0, gradientLayer.toList()) // Adding backwards because we are propagating backwards
            activationDerivatives = nextActivationDerivatives.toList()
        }


        return Network(out.toList())
    }

    @JvmName("fireAndComputeGradient")
    fun computeGradient(inputs: List1d, key: List1d) = computeGradient(fire(inputs), key)

    fun descend(gradient: Network, step: Double) = this + gradient * -step

    override fun toString(): String {
        val sigfigs = MathContext(1)

        fun Double.formatted(): String {
            val rounded = toBigDecimal().round(sigfigs).toDouble().toString()
            var decimalPos: Int? = null
            var digitPos: Int? = null
            val negative = this < 0
            for (i in rounded.indices) {
                when (rounded[i]) {
                    '0' -> {}
                    '.' -> decimalPos = i
                    else -> digitPos = i
                }
            }

            if (digitPos == null) return " 0   "
            if (decimalPos == null) return "${rounded}e0 "

            val e = (decimalPos - digitPos).toString()
            return "${if (negative) "-" else " "}${rounded[digitPos]}e$e${" " * (2 - e.length).coerceAtLeast(0)}"
        }

        val layers: List<List<String>> = network.map { layer ->
            val out = mutableListOf<String>()
            if (layer.size > 1) {
                out += buildString {
                    append("┌ ")
                    layer[0].forEach { weight -> append("${weight.formatted()} ") }
                    append("┐")
                }
                for (i in 1 until layer.size - 1) {
                    out += buildString {
                        append("│ ")
                        layer[i].forEach { weight -> append("${weight.formatted()} ") }
                        append("│")
                    }
                }
                out += buildString {
                    append("└ ")
                    layer.last().forEach { weight -> append("${weight.formatted()} ") }
                    append("┘")
                }
            } else {
                out += buildString {
                    append("[ ")
                    layer[0].forEach { weight -> append("${weight.formatted()} ") }
                    append("]")
                }
            }
            out.toList()
        }

        val maxRows = nodes.coerceAtLeast(outputs)
        val midPoint = (maxRows - 1) / 2.0
        var out = ""
        for (i in 0 until maxRows) {
            out += buildString {
                layers.forEach { layer ->
                    val layerMidPoint = (layer.size - 1) / 2.0
                    val range = (midPoint - layerMidPoint).toInt()..(midPoint + layerMidPoint).toInt()
                    if (i in range) {
                        append(layer[i - (midPoint - layerMidPoint).toInt()])
                    } else {
                        append(" " * layer[0].length)
                    }
                }
                append("\n")
            }
        }
        return out
    }

    val serialized: String get() = network.toString()

    companion object {
        fun deserialize(serial: String) = Network(
            splitList(serial, '[', ']').map { layer ->
                splitList(layer, '[', ']').map { node ->
                    splitList(node, '[', ']').map { weight ->
                        weight.toDouble()
                    }
                }
            }
        )

        fun random(inputs: Int, layers: Int, nodes: Int, outputs: Int) = Network(buildList {
            add((0 until nodes).map { // node
                (0 until inputs + 1).map { // weight
                    (Math.random() * 2.0 - 1.0) * sqrt(2.0 / (inputs + 1.0))
                }
            })
            addAll((1 until layers).map { // layer
                (0 until nodes).map { // node
                    (0 until nodes + 1).map { // weight
                        (Math.random() * 2.0 - 1.0)  * sqrt(2.0 / (nodes + 1.0))
                    }
                }
            })
            add((0 until outputs).map { // node
                (0 until nodes + 1).map { // weight
                    (Math.random() * 2.0 - 1.0) * sqrt(2.0 / (nodes + 1.0))
                }
            })
        }, inputs, layers, nodes, outputs)

        fun zero(inputs: Int, layers: Int, nodes: Int, outputs: Int) = Network(buildList {
            add((0 until nodes).map { // node
                (0 until inputs + 1).map { // weight
                    0.0
                }
            })
            addAll((1 until layers).map { // layer
                (0 until nodes).map { // node
                    (0 until nodes + 1).map { // weight
                        0.0
                    }
                }
            })
            add((0 until outputs).map { // node
                (0 until nodes + 1).map { // weight
                    0.0
                }
            })
        }, inputs, layers, nodes, outputs)

        fun one(inputs: Int, layers: Int, nodes: Int, outputs: Int) = Network(buildList {
            add((0 until nodes).map { // node
                (0 until inputs + 1).map { // weight
                    1.0
                }
            })
            addAll((1 until layers).map { // layer
                (0 until nodes).map { // node
                    (0 until nodes + 1).map { // weight
                        1.0
                    }
                }
            })
            add((0 until outputs).map { // node
                (0 until nodes + 1).map { // weight
                    1.0
                }
            })
        }, inputs, layers, nodes, outputs)
    }

    operator fun times(scale: Double): Network = Network(network * scale, inputs, layers, nodes, outputs)
    operator fun div(divisor: Double) = this * (1.0 / divisor)
    operator fun plus(other: Network): Network {
        require(inputs == other.inputs && layers == other.layers && nodes == other.nodes && outputs == other.outputs){
            "This network does not match dimensions of other network"
        }
        return Network(network + other.network, inputs, layers, nodes, outputs)
    }
    operator fun minus(other: Network): Network {
        require(inputs == other.inputs && layers == other.layers && nodes == other.nodes && outputs == other.outputs){
            "This network does not match dimensions of other network"
        }
        return Network(network - other.network, inputs, layers, nodes, outputs)
    }
}

@JvmName("scale3d")
operator fun List3d.times(scale: Double) = map { it * scale }

@JvmName("plus3d")
operator fun List3d.plus(other: List3d) = mapIndexed { i, it -> it + other[i] }

@JvmName("minus3d")
operator fun List3d.minus(other: List3d) = mapIndexed { i, it -> it - other[i] }


@JvmName("scale2d")
operator fun List2d.times(scale: Double): List2d = map { it ->
    it.map {
        it * scale
    }
}

val List2d.transpose: List2d
    get() {

        require(rectangular) {
            "2d List must be a rectangular matrix"
        }

        val builder = mutableListOf<List1d>()
        for (j in this[0].indices) {
            val column = mutableListOf<Double>()
            for (i in indices) {
                column.add(this[i][j])
            }
            builder.add(column.toList())
        }

        return builder.toList()
    }

operator fun List2d.times(other: List2d): List2d {
    require(this[0].size == other.size) {
        "Matrix columns ${this[0].size} must equal rows ${other.size}"
    }
    return other.transpose.map {
        this * it
    }.transpose
}

@JvmName("apply")
operator fun List2d.times(vector: List1d): List1d {
    require(this[0].size == vector.size) {
        "Vector dimension of ${vector.size} does not match with transformation dimension of ${this[0].size}"
    }
    val transposed = transpose
    var sum = map { 0.0 }
    for (i in vector.indices) {
        sum = sum + (transposed[i] * vector[i])
    }
    return sum
}

val List2d.rectangular: Boolean get() = map { size }.distinct().size == 1

@JvmName("plus2d")
operator fun List2d.plus(other: List2d) = mapIndexed { i, it -> it + other[i] }

@JvmName("minus2d")
operator fun List2d.minus(other: List2d) = mapIndexed { i, it -> it - other[i] }


@JvmName("scale")
operator fun List1d.times(scale: Double): List1d = this.map { it * scale }

operator fun List1d.plus(other: List1d): List1d {
    require(size == other.size) {
        "List size $size does not match with other list size ${other.size}"
    }

    return mapIndexed { i, it ->
        it + other[i]
    }
}

operator fun List1d.minus(other: List1d): List1d {
    require(size == other.size) {
        "List size $size does not match with other list size ${other.size}"
    }

    return mapIndexed { i, it ->
        it - other[i]
    }
}


operator fun String.times(scale: Int) = buildString {
    repeat(scale) {
        append(this@times)
    }
}

fun splitList(serial: String, open: Char, close: Char): List<String> = buildList {
    var net = 0
    var lastSplit = 1
    val serial = serial.filter { !it.isWhitespace() }
    for (i in serial.indices) {
        when (serial[i]) {
            open -> net++
            close -> net--
            ',' -> if (net == 1) {
                add(serial.substring(lastSplit, i))
                lastSplit = i + 1
            }
        }
    }
    add(serial.substring(lastSplit, serial.length - 1))
}