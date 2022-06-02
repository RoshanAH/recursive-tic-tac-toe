package com.roshanah.recursiveTac.ml

import java.lang.IllegalArgumentException
import kotlin.math.sqrt

class Network internal constructor(inputs: List<() -> Double>, network: CompressedNetwork) {
    val inputs: List<Node>
    val outputs: List<Node>
    val layers: List<List<Node>>


    private fun requireDimensions(inputs: Int, hiddenLayers: Int, hiddenNodes: Int, outputs: Int) {
        if (hiddenNodes == 0) require(hiddenLayers == 0) { "Cannot have empty hidden layers" }
        require(inputs > 0) { "Network must have at least 1 input" }
        require(outputs > 0) { "Network must have at least 1 output" }
    }


    val compressed get() = CompressedNetwork(
        inputs.size,
        layers.size,
        if (layers.isEmpty()) 0 else layers[0].size,
        outputs.size,
        buildList {
            for (l in layers) {
                for (n in l) {
                    add(CompressedNetwork.Component(n.bias, n.receivers.map { it.weight }))
                }
            }

            for (n in outputs) {
                add(CompressedNetwork.Component(n.bias, n.receivers.map { it.weight }))
            }
        })

    val zero get() = CompressedNetwork(
        inputs.size,
        layers.size,
        if (layers.isEmpty()) 0 else layers[0].size,
        outputs.size,
        buildList{
            for (l in layers) {
                for (n in l) {
                    add(CompressedNetwork.Component(0.0, n.receivers.map { 0.0 }))
                }
            }

            for (n in outputs) {
                add(CompressedNetwork.Component(0.0, n.receivers.map { 0.0 }))
            }
        }
    )

    val one get() = CompressedNetwork(
        inputs.size,
        layers.size,
        if (layers.isEmpty()) 0 else layers[0].size,
        outputs.size,
        buildList{
            for (l in layers) {
                for (n in l) {
                    add(CompressedNetwork.Component(1.0, n.receivers.map { 1.0 }))
                }
            }

            for (n in outputs) {
                add(CompressedNetwork.Component(1.0, n.receivers.map { 1.0 }))
            }
        }
    )



    override fun toString() = compressed.toString()

//    fun formattedString() {
//
//    }

    fun descend(gradient: CompressedNetwork, step: Double, inputs: List<() -> Double>) = (compressed - gradient * step).extract(inputs)
    fun descend(gradient: CompressedNetwork, step: Double) = (compressed - gradient * step)

    fun computeGradient(key: List<Double?>): CompressedNetwork {
        require(key.size == outputs.size) { "Key size does not match with output size" }
        val subGradients = outputs.mapIndexed{ i, output ->
            val outputKey = key[i]
            if(outputKey == null) output.zeroGradient()
            else output.computeGradient().map { it * (output.fire() - outputKey) }
        }



        return CompressedNetwork(inputs.size, layers.size, if(layers.isEmpty()) 0 else layers[0].size, outputs.size, buildList {
            val first = subGradients[0]
            var sum = first.subList(0, first.size - 1)
            for(i in 1 until subGradients.size){
                val it = subGradients[i]
                sum = it.subList(0, it.size - 1).mapIndexed { j, it ->
                    sum[j] + it
                }
            }

            addAll(sum)

            subGradients.forEach {
                add(it.last())
            }
        })
    }

    private fun reset() {
        inputs.forEach { it.reset() }
        outputs.forEach { it.reset() }
        layers.forEach { list -> list.forEach { node -> node.reset() } }
    }

    fun fire(): List<Double> {
        reset()
        return outputs.map { it.fire() }
    }

    init {
        requireDimensions(inputs.size, network.layers, network.layerNodes, network.outputs)
        require(inputs.size == network.inputs) { "Input size does not match compressed network input size" }
        this.inputs = inputs.map { Node(it) }
        layers = if (network.layers > 0) buildList {
            add(buildList {
                for (i in 0 until network.layerNodes) {
                    add(Node(network[i].bias,
                        this@Network.inputs.mapIndexed { j, it ->
                            Connection(network[i].weights[j], it)
                        }
                    ))
                }
            })

            for (i in 1 until network.layers) {
                val last = last()
                add(buildList {
                    for (j in 0 until network.layerNodes) {
                        val component = network[i * network.layerNodes + j]
                        add(Node(component.bias,
                            last.mapIndexed { k, it ->
                                Connection(component.weights[k], it)
                            }
                        ))
                    }
                })
            }

        } else emptyList()
        outputs = if (layers.isNotEmpty()) buildList {
            for (i in 0 until network.outputs) {
                val component = network[network.layers * network.layerNodes + i]
                add(Node(component.bias,
                    layers.last().mapIndexed { j, it ->
                        Connection(component.weights[j], it)
                    }
                ))
            }
        } else buildList {
            for (i in 0 until network.outputs) {
                val component = network[network.layers * network.layerNodes + i]
                add(Node(component.bias,
                    this@Network.inputs.mapIndexed { j, it ->
                        Connection(component.weights[j], it)
                    }
                ))
            }
        }
    }

}

class CompressedNetwork(
    val inputs: Int,
    val layers: Int,
    val layerNodes: Int,
    val outputs: Int,
    private val components: List<Component>
) : Collection<CompressedNetwork.Component> {

    data class Component(val bias: Double, val weights: List<Double>) {
        operator fun plus(other: Component): Component {
            require(weights.size == other.weights.size) { "Layer sizes ${weights.size} and ${other.weights.size} are different" }
            return Component(bias + other.bias, weights.mapIndexed { i, it ->
                it + other.weights[i]
            })
        }

        operator fun minus(other: Component): Component {
            require(weights.size == other.weights.size) { "Layer sizes ${weights.size} and ${other.weights.size} are different" }
            return Component(bias - other.bias, weights.mapIndexed { i, it ->
                it - other.weights[i]
            })
        }

        operator fun times(scale: Number) =
            Component(bias * scale.toDouble(), weights.map { it * scale.toDouble() })

        operator fun div(divisor: Number) = this * (1.0 / divisor.toDouble())

        override fun toString(): String {
            var out = "$bias["
            weights.forEach { out += "$it," }
            return "$out]"
        }
    }

    companion object {
        fun deserialize(serial: String): CompressedNetwork {
            val serial = serial.filter { !it.isWhitespace() }
            val dimensions = (
                    """(?<=\().*(?=\))""".toRegex().find(serial)
                        ?: throw IllegalArgumentException("Invalid network serial format: Cannot find dimensions")
                    ).value.split(",").map { it.toInt() }
            require(dimensions.size == 4) { "Invalid network serial format: Incorrect amount of dimensions" }


            val components = buildList {
                ("""(?<=\)\[).*(?=,])""".toRegex().find(serial)
                    ?: throw IllegalArgumentException("Invalid serial network format: Cannot find nodes in $serial")
                        ).value.split("""(?<=]),""".toRegex()).forEach {
                        add(
                            Component(
                                ("""[\d|.]+(?=\[)""".toRegex().find(it)
                                    ?: throw IllegalArgumentException("Invalid network serial format: Cannot find bias in $it")
                                        ).value.toDouble(),
                                ("""(?<=\[).*(?=,])""".toRegex().find(it)
                                    ?: throw IllegalArgumentException("Invalid network serial format: Cannot find weights in $it")
                                        ).value.split(",").map { it.toDouble() }
                            )
                        )
                    }
            }
            return CompressedNetwork(dimensions[0], dimensions[1], dimensions[2], dimensions[3], components)
        }

        fun zero(inputs: Int, layers: Int, nodes: Int, outputs: Int) = CompressedNetwork(
            inputs,
            layers,
            if (layers == 0) 0 else nodes,
            outputs,
            buildList {
                for (n in 0 until nodes) {
                    add(Component(0.0, (0 until inputs).map { 0.0 }))
                }
                for (l in 1 until layers) {
                    for (n in 0 until nodes) {
                        add(Component(0.0, (0 until nodes).map { 0.0 }))
                    }
                }
                for (n in 0 until outputs) {
                    add(Component(0.0, (0 until if(layers == 0 ) inputs else nodes).map { 0.0 }))
                }
            })

        fun one(inputs: Int, layers: Int, nodes: Int, outputs: Int) = CompressedNetwork(
            inputs,
            layers,
            if (layers == 0) 0 else nodes,
            outputs,
            buildList {
                for (n in 0 until nodes) {
                    add(Component(1.0, (0 until inputs).map { 1.0 }))
                }
                for (l in 1 until layers) {
                    for (n in 0 until nodes) {
                        add(Component(1.0, (0 until nodes).map { 1.0 }))
                    }
                }
                for (n in 0 until outputs) {
                    add(Component(1.0, (0 until if(layers == 0 ) inputs else nodes).map { 1.0 }))
                }
            })

        fun random(inputs: Int, layers: Int, nodes: Int, outputs: Int) = CompressedNetwork(
            inputs,
            layers,
            if (layers == 0) 0 else nodes,
            outputs,
            buildList {
                for (n in 0 until nodes) {
                    add(Component(Math.random() / sqrt(nodes.toDouble()), (0 until inputs).map { Math.random() / sqrt(nodes.toDouble()) }))
                }
                for (l in 1 until layers) {
                    for (n in 0 until nodes) {
                        add(Component(Math.random() / sqrt(nodes.toDouble()), (0 until nodes).map { Math.random() / sqrt(nodes.toDouble())}))
                    }
                }
                for (n in 0 until outputs) {
                    add(Component(Math.random() / sqrt(outputs.toDouble()), (0 until if(layers == 0 ) inputs else nodes).map { Math.random() / sqrt(outputs.toDouble()) }))
                }
            })

    }

    private fun requireDimensions(other: CompressedNetwork) = require(
        size == other.size &&
                inputs == other.inputs &&
                layers == other.layers &&
                layerNodes == other.layerNodes &&
                outputs == other.outputs
    ) {
        "Compressed network dimensions do not match"
    }

    fun extract(inputs: List<() -> Double>) = Network(inputs, this)

    operator fun plus(other: CompressedNetwork): CompressedNetwork {
        requireDimensions(other)
        return CompressedNetwork(inputs, layers, layerNodes, outputs, mapIndexed { i, it -> it + other[i] })
    }

    operator fun minus(other: CompressedNetwork): CompressedNetwork {
        requireDimensions(other)
        return CompressedNetwork(inputs, layers, layerNodes, outputs, mapIndexed { i, it -> it - other[i] })
    }

    operator fun get(index: Int) = components[index]

    operator fun times(scale: Number) = CompressedNetwork(inputs, layers, layerNodes, outputs, map { it * scale })
    operator fun div(divisor: Number) = CompressedNetwork(inputs, layers, layerNodes, outputs, map { it / divisor })

    override val size: Int get() = components.size

    override fun contains(element: Component) = components.contains(element)

    override fun containsAll(elements: Collection<Component>) = components.containsAll(elements)

    override fun isEmpty() = components.isEmpty()

    override fun iterator() = components.iterator()

    val random get() = CompressedNetwork.random(inputs, layers, layerNodes, outputs)

    override fun toString(): String {
        var out = "(${inputs},${layers},${if (layers == 0) 0 else layerNodes},${outputs})["
        forEach { out += "${it}," }
        return "$out]"
    }

    fun formattedString(): String{
        var out = "(${inputs}, ${layers}, ${if (layers == 0) 0 else layerNodes}, ${outputs})[\n"
        forEach { out += "  ${it},\n" }
        return "$out]"
    }
}





