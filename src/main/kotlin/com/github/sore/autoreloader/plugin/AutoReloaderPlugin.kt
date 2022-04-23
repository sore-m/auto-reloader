package com.github.sore.autoreloader.plugin

import net.kyori.adventure.text.Component.space
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * @author Monun
 */

class AutoReloaderPlugin : JavaPlugin() {
    private lateinit var updateAction: UpdateAction
    private var countdownTicks = 40

    private lateinit var watchlist: MutableList<Pair<Plugin, File>>

    override fun onEnable() {
        saveDefaultConfig()

        val config = config
        updateAction = UpdateAction.valueOf(config.getString("update-action")!!.uppercase())
        countdownTicks = config.getInt("countdown-ticks")

        server.scheduler.let { scheduler ->
            scheduler.runTask(this, Runnable {
                watchlist = server.pluginManager.plugins.mapTo(ArrayList()) { plugin ->
                    val file = plugin.file
                    val updateFolder = File(file.parentFile, "update")
                    plugin to File(updateFolder, file.name)
                }.onEach { (plugin, file) ->
                    logger.info("- ${plugin.name}: ${file.path}")
                }
            })

            scheduler.runTaskTimer(this, this::watch, 1L, 1L)
        }
    }

    private var updateTicks = 0

    private fun watch() {
        if (updateTicks > 0 && --updateTicks <= 0) {
            val server = server
            val console = server.consoleSender
            Bukkit.dispatchCommand(console, updateAction.commands)
        }

        watchlist.removeIf { update ->
            if (update.second.exists()) {
                server.sendMessage(
                    text().content("Found update file for plugin").color(NamedTextColor.LIGHT_PURPLE)
                        .append(space())
                        .append(
                            text().content(update.first.name).color(NamedTextColor.GOLD)
                                .decorate(TextDecoration.ITALIC).decorate(TextDecoration.UNDERLINED)
                        )
                )
                server.sendMessage(
                    text().color(NamedTextColor.YELLOW).content("Server will ")
                        .append(text().content(updateAction.message))
                        .append(text().content(" in ")).append(text((countdownTicks + 19) / 20))
                        .append(text().content(" seconds"))
                )
                updateTicks = countdownTicks
                true
            } else false
        }
    }
}

private val Plugin.file: File
    get() {
        return JavaPlugin::class.java.getDeclaredField("file").apply {
            isAccessible = true
        }.get(this) as File
    }

enum class UpdateAction(val commands: String, val message: String) {
    RELOAD("reload confirm", "reload"),
    RESTART("restart", "restart"),
    SHUTDOWN("stop", "shutdown");
}