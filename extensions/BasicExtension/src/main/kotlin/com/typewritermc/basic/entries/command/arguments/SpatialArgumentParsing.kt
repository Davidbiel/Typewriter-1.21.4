package com.typewritermc.basic.entries.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.typewritermc.engine.paper.command.dsl.error
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector as BukkitVector
import java.util.UUID
import java.util.concurrent.CompletableFuture

abstract class SpatialArgumentType<T : Any> : CustomArgumentType<T, String> {
    override fun parse(reader: StringReader): T {
        throw UnsupportedOperationException("This method will never be called.")
    }

    override fun getNativeType(): ArgumentType<String> = StringArgumentType.greedyString()

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = builder.buildFuture()
}

data class ParsedCoordinates(
    val x: Double,
    val y: Double,
    val z: Double,
)

data class ParsedRotation(
    val yaw: Float,
    val pitch: Float,
)

fun requireCommandSourceStack(source: Any): CommandSourceStack {
    if (source !is CommandSourceStack) {
        throw IllegalArgumentException("Source must be a CommandSourceStack")
    }
    return source
}

fun parseWorld(reader: StringReader, source: CommandSourceStack): World {
    val token = readToken(reader, "world")
    return source.sender.server.getWorld(token)
        ?: runCatching { UUID.fromString(token) }
            .getOrNull()
            ?.let(source.sender.server::getWorld)
        ?: reader.error("Unknown world '$token'")
}

fun parseCoordinates(reader: StringReader, source: CommandSourceStack): ParsedCoordinates {
    val xToken = readToken(reader, "x")
    val yToken = readToken(reader, "y")
    val zToken = readToken(reader, "z")
    return parseCoordinates(source.location, reader, xToken, yToken, zToken)
}

fun parseRotation(reader: StringReader, source: CommandSourceStack): ParsedRotation {
    val yawToken = readToken(reader, "yaw")
    val pitchToken = readToken(reader, "pitch")
    return ParsedRotation(
        parseRotationToken(reader, yawToken, source.location.yaw, "yaw"),
        parseRotationToken(reader, pitchToken, source.location.pitch, "pitch"),
    )
}

fun ensureFullyConsumed(reader: StringReader) {
    skipWhitespace(reader)
    if (reader.canRead()) {
        reader.error("Unexpected trailing input")
    }
}

fun <S : Any> suggestWorlds(
    context: CommandContext<S>,
    builder: SuggestionsBuilder,
): CompletableFuture<Suggestions> {
    val source = context.source as? CommandSourceStack ?: return builder.buildFuture()
    val input = builder.remaining
    if (input.contains(' ')) {
        return builder.buildFuture()
    }

    source.sender.server.worlds
        .asSequence()
        .map(World::getName)
        .filter { it.startsWith(input, ignoreCase = true) }
        .forEach(builder::suggest)

    return builder.buildFuture()
}

private fun parseCoordinates(
    base: Location,
    reader: StringReader,
    xToken: String,
    yToken: String,
    zToken: String,
): ParsedCoordinates {
    val usesLocal = listOf(xToken, yToken, zToken).any { it.startsWith("^") }
    if (usesLocal) {
        if (listOf(xToken, yToken, zToken).any { !it.startsWith("^") }) {
            reader.error("Local coordinates must use ^ for x, y and z")
        }
        return parseLocalCoordinates(base, reader, xToken, yToken, zToken)
    }

    return ParsedCoordinates(
        parseCoordinateToken(reader, xToken, base.x, "x"),
        parseCoordinateToken(reader, yToken, base.y, "y"),
        parseCoordinateToken(reader, zToken, base.z, "z"),
    )
}

private fun parseLocalCoordinates(
    base: Location,
    reader: StringReader,
    xToken: String,
    yToken: String,
    zToken: String,
): ParsedCoordinates {
    val localX = parseLocalComponent(reader, xToken, "x")
    val localY = parseLocalComponent(reader, yToken, "y")
    val localZ = parseLocalComponent(reader, zToken, "z")

    val forward = base.direction.clone().normalize()
    var left = forward.clone().crossProduct(BukkitVector(0, 1, 0))
    if (left.lengthSquared() == 0.0) {
        left = fallbackLeftVector(base.yaw)
    } else {
        left.normalize()
    }

    var up = left.clone().crossProduct(forward)
    if (up.lengthSquared() == 0.0) {
        up = BukkitVector(0, 1, 0)
    } else {
        up.normalize()
    }

    val offset = left.multiply(localX)
        .add(up.multiply(localY))
        .add(forward.multiply(localZ))

    return ParsedCoordinates(
        base.x + offset.x,
        base.y + offset.y,
        base.z + offset.z,
    )
}

private fun fallbackLeftVector(yaw: Float): BukkitVector {
    val yawRadians = Math.toRadians(yaw.toDouble())
    return BukkitVector(-kotlin.math.cos(yawRadians), 0.0, kotlin.math.sin(yawRadians)).normalize()
}

private fun parseCoordinateToken(
    reader: StringReader,
    token: String,
    base: Double,
    name: String,
): Double {
    if (token.startsWith("^")) {
        reader.error("Local coordinates are only supported when x, y and z all use ^")
    }
    return if (token.startsWith("~")) {
        base + parseOptionalDouble(reader, token.drop(1), name)
    } else {
        parseDouble(reader, token, name)
    }
}

private fun parseRotationToken(
    reader: StringReader,
    token: String,
    base: Float,
    name: String,
): Float {
    if (token.startsWith("^")) {
        reader.error("Rotation does not support ^ coordinates")
    }
    return if (token.startsWith("~")) {
        base + parseOptionalFloat(reader, token.drop(1), name)
    } else {
        parseFloat(reader, token, name)
    }
}

private fun parseLocalComponent(
    reader: StringReader,
    token: String,
    name: String,
): Double = parseOptionalDouble(reader, token.drop(1), name)

private fun parseOptionalDouble(
    reader: StringReader,
    value: String,
    name: String,
): Double {
    if (value.isBlank()) return 0.0
    return parseDouble(reader, value, name)
}

private fun parseOptionalFloat(
    reader: StringReader,
    value: String,
    name: String,
): Float {
    if (value.isBlank()) return 0f
    return parseFloat(reader, value, name)
}

private fun parseDouble(
    reader: StringReader,
    value: String,
    name: String,
): Double = value.toDoubleOrNull() ?: reader.error("Invalid $name value '$value'")

private fun parseFloat(
    reader: StringReader,
    value: String,
    name: String,
): Float = value.toFloatOrNull() ?: reader.error("Invalid $name value '$value'")

private fun readToken(reader: StringReader, name: String): String {
    skipWhitespace(reader)
    if (!reader.canRead()) {
        reader.error("Expected $name")
    }

    val start = reader.cursor
    while (reader.canRead() && reader.peek() != ' ') {
        reader.skip()
    }

    return reader.string.substring(start, reader.cursor)
}

private fun skipWhitespace(reader: StringReader) {
    while (reader.canRead() && reader.peek() == ' ') {
        reader.skip()
    }
}
