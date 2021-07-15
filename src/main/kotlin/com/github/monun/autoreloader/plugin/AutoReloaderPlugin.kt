package com.github.monun.autoreloader.plugin

import io.github.monun.kommand.kommand
import io.github.monun.tap.config.Config
import io.github.monun.tap.config.ConfigSupport
import io.github.monun.tap.config.RangeInt
import io.github.monun.tap.util.GitHubSupport
import io.github.monun.tap.util.updateFromGitHubMagically
import kotlinx.coroutines.DelicateCoroutinesApi
import net.kyori.adventure.text.Component.space
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * @author Monun
 */

@DelicateCoroutinesApi
class AutoReloaderPlugin : JavaPlugin(), Runnable {
    private lateinit var updates: MutableList<Pair<Plugin, File>>

    @Config
    private var updateAction = UpdateAction.RELOAD

    @Config
    @RangeInt(min = 0)
    private var countdownTicks = 40

    override fun onEnable() {
        ConfigSupport.computeConfig(this, File(dataFolder, "config.yml"))

        server.scheduler.let { scheduler ->
            scheduler.runTaskTimer(this, this, 1L, 1L)
            scheduler.runTask(this, Runnable {
                updates = server.pluginManager.plugins.mapTo(ArrayList()) { plugin ->
                    val file = plugin.file
                    val updateFolder = File(file.parentFile, "update")
                    plugin to File(updateFolder, file.name)
                }
            })
        }

        kommand {
            register("autoreloader") {
                then("update") {
                    executes {
                        updateFromGitHubMagically("monun", "auto-reloader", "AutoReloader.jar") {
                            sender.sendMessage(text(it))
                        }
                    }
                }
            }
        }
    }

    private var updateTicks = 0

    override fun run() {
        if (updateTicks > 0 && --updateTicks <= 0) {
            val server = server
            when (updateAction) {
                UpdateAction.RELOAD -> {
                    server.dispatchCommand(server.consoleSender, "reload confirm")
                }
                UpdateAction.STOP -> {
                    server.dispatchCommand(server.consoleSender, "stop")
                }
                UpdateAction.RESTART -> {
                    server.dispatchCommand(server.consoleSender, "restart")
                }
            }
        }

        updates.removeIf { update ->
            if (update.second.exists()) {
                server.sendMessage(
                    text().content("Found update file for plugin").color(NamedTextColor.LIGHT_PURPLE)
                        .append(space())
                        .append(text().content(update.first.name).color(NamedTextColor.GOLD))
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

enum class UpdateAction {
    RELOAD,
    STOP,
    RESTART
}