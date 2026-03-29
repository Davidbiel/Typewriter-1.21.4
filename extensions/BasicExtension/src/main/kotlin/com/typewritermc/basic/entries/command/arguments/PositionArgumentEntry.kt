package com.typewritermc.basic.entries.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.typewritermc.basic.entries.command.ArgumentCommandArgument
import com.typewritermc.basic.entries.command.CommandArgumentEntry
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.ContextKeys
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.KeyType
import com.typewritermc.core.interaction.EntryContextKey
import com.typewritermc.core.interaction.InteractionContextBuilder
import com.typewritermc.core.utils.point.*
import com.typewritermc.engine.paper.command.dsl.ExecutionContext
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.utils.toWorld
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

@Entry("position_argument", "A position argument", Colors.RED, "majesticons:map-marker-area")
@ContextKeys(PositionArgumentContextKeys::class)
/**
 * The `Position Argument` entry is an argument that takes a position.
 *
 * It can be used to specify a position in a world.
 *
 * ## How could this be used?
 * This could be used to create commands that require a position, such as teleporting to a specific location.
 */
class PositionArgumentEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<CommandArgumentEntry>> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : ArgumentCommandArgument<Position> {
    override val type: ArgumentType<Position> get() = PositionArgument()
    override val klass: KClass<Position> get() = Position::class

    override fun InteractionContextBuilder.apply(context: ExecutionContext<CommandSourceStack>, value: Position) {
        set(PositionArgumentContextKeys.POSITION, value)
        set(PositionArgumentContextKeys.COORDINATE, value.toCoordinate())
        set(PositionArgumentContextKeys.VECTOR, value.toVector())
    }
}

enum class PositionArgumentContextKeys(override val klass: KClass<*>) : EntryContextKey {
    @KeyType(Position::class)
    POSITION(Position::class),

    @KeyType(Coordinate::class)
    COORDINATE(Coordinate::class),

    @KeyType(Vector::class)
    VECTOR(Vector::class),
}

class PositionArgument : SpatialArgumentType<Position>() {
    override fun <S : Any> parse(reader: StringReader, source: S): Position {
        val stack = requireCommandSourceStack(source)
        val world = parseWorld(reader, stack)
        val position = parseCoordinates(reader, stack)
        val rotation = parseRotation(reader, stack)
        ensureFullyConsumed(reader)

        return Position(world.toWorld(), position.x, position.y, position.z, rotation.yaw, rotation.pitch)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = suggestWorlds(context, builder)
}

fun <T : Any, S : Any> ArgumentType<T>.canParse(source: S, reader: StringReader): Boolean {
    return try {
        parse<S>(reader, source)
        true
    } catch (_: Exception) {
        false
    }
}
