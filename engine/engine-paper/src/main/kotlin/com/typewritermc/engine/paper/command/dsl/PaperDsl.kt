@file:Suppress("UnstableApiUsage")

package com.typewritermc.engine.paper.command.dsl

import com.typewritermc.core.entries.Entry
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.loader.Extension
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

typealias CommandTree = DslCommandTree<CommandSourceStack, *>

val ExecutionContext<CommandSourceStack>.sender: CommandSender
    get() = source.sender

fun CommandTree.withPermission(permission: String): CommandTree {
    requires { it.sender.hasPermission(permission) }
    return this
}

fun CommandTree.requiresPlayer() {
    requires { it.sender is Player || it.executor is Player }
}

fun <S> DslCommandTree<S, *>.playerResolver(
    name: String,
    block: ArgumentBlock<S, String> = {},
) = word(name, block)


fun <S> DslCommandTree<S, *>.playersResolver(
    name: String,
    block: ArgumentBlock<S, String> = {},
) = word(name, block)

fun ExecutionContext<CommandSourceStack>.resolvePlayers(target: String): List<Player> = try {
    sender.server.selectEntities(sender, target).filterIsInstance<Player>()
} catch (_: IllegalArgumentException) {
    emptyList()
}

fun CommandTree.executePlayerOrTarget(block: ExecutionContext<CommandSourceStack>.(Player) -> Unit) {
    playersResolver("target") { target ->
        executes {
            val players = resolvePlayers(target())
            if (players.isEmpty()) {
                sender.msg("No players found.")
                return@executes
            }
            players.forEach { player ->
                block(this, player)
            }
        }
    }

    executePlayer(block)
}


fun CommandTree.executePlayer(block: ExecutionContext<CommandSourceStack>.(Player) -> Unit) {
    executes {
        (source.executor as? Player)?.let { player ->
            block(this, player)
            return@executes
        }
        (sender as? Player)?.let { player ->
            block(this, player)
            return@executes
        }
        sender.msg("You must be a player to execute this command!")
    }
}

inline fun <reified E : Entry> CommandTree.entry(
    name: String,
    noinline filter: Predicate<E> = { true },
    noinline block: ArgumentBlock<CommandSourceStack, E> = {},
) = argument(name, EntryArgumentType(E::class, filter), E::class, block)

inline fun <reified E : Extension> CommandTree.extension(
    name: String,
    noinline block: ArgumentBlock<CommandSourceStack, E> = {},
) = argument(name, ExtensionArgumentType(E::class), E::class, block)
