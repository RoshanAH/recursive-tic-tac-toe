package com.roshanah.recursiveTac.ml

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

typealias List1d = List<Double>
typealias List2d = List<List1d>
typealias List3d = List<List2d>

class Network(val network: List3d) {
    val inputs: Int
    val layers: Int
    val nodes: Int
    val outputs: Int

    init {
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
    }

    fun fire(inputs: List1d): List2d = buildList {
        require(inputs.size == this@Network.inputs){
            "Input size ${inputs.size} does not match network input size ${this@Network.inputs}"
        }
        add(inputs.map { it.coerceAtLeast(0.0) } + 1.0)
        for (i in 0 until network.size - 1) {
            add(network[i] * (this[i] + 1.0).map { it.coerceAtLeast(0.0) })
        }
    }

    override fun toString(): String {
        val sigfigs = MathContext(1)

        val line = "│"
        val topLeft = "┌"
        val topRight = "┐"
        val bottomRight = "┘"
        val bottomLeft = "└"

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

            if (digitPos == null) return "0   "
            if (decimalPos == null) return "${rounded}e0 "

            val e = (decimalPos - digitPos).toString()
            return "${if (negative) "-" else " "}${rounded[digitPos]}e$e${" " * (2 - e.length).coerceAtLeast(0)}"
        }

        val layers: List<List<String>> = network.map { layer ->
            val out = mutableListOf<String>()
            if(layer.size > 1) {
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
            }else{
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
        for(i in 0 until maxRows){
            out += buildString {
                layers.forEach { layer ->
                    val layerMidPoint = (layer.size - 1) / 2.0
                    val range = (midPoint - layerMidPoint).toInt()..(midPoint + layerMidPoint).toInt()
                    if (i in range) {
                        append(layer[i - (midPoint - layerMidPoint).toInt()])
                    }else {
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
//        fun deserialize(serial: String): Network {
//
//        }

        fun random(inputs: Int, layers: Int, nodes: Int, outputs: Int) = Network(buildList {
            add((0 until nodes).map { // node
                (0 until inputs + 1).map { // weight
                    Math.random() / sqrt(inputs + 1.0)
                }
            })
            addAll((1 until layers).map { // layer
                (0 until nodes).map { // node
                    (0 until nodes + 1).map { // weight
                        Math.random() / sqrt(nodes + 1.0)
                    }
                }
            })
            add((0 until outputs).map { // node
                (0 until nodes + 1).map { // weight
                    Math.random() / sqrt(nodes + 1.0)
                }
            })
        })

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
        })

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
        })
    }

    operator fun times(scale: Double): Network = Network(network * scale)
    operator fun plus(other: Network): Network = Network(network + other.network)
}

@JvmName("scale3d")
operator fun List3d.times(scale: Double) = map { it * scale }

@JvmName("plus3d")
operator fun List3d.plus(other: List3d) = mapIndexed { i, it -> it + other[i] }



@JvmName("scale2d")
operator fun List2d.times(scale: Double): List2d = map { it ->
    it.map {
        it * scale
    }
}

val List2d.transpose: List2d get() {

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
operator fun List2d.times(other: List1d): List1d {
    require(this[0].size == other.size) {
        "Dimension of vector ${other.size} does not match with input dimension of transformation ${this[0].size}"
    }
    val transposed = transpose
    var sum = other.map { 0.0 }
    for (i in other.indices) {
        sum = sum + (transposed[i] * other[i])
    }
    return sum
}

val List2d.rectangular: Boolean get() = map { size }.distinct().size == 1

@JvmName("plus2d")
operator fun List2d.plus(other: List2d) = mapIndexed {i, it -> it + other[i]}



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



operator fun String.times(scale: Int) = buildString {
    repeat(scale) {
        append(this@times)
    }
}