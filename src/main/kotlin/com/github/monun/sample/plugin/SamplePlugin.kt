package com.github.monun.sample.plugin

import io.github.monun.kommand.Kommand
import net.kyori.adventure.text.Component.text
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Monun
 */
class SamplePlugin : JavaPlugin() {
    override fun onEnable() {
        Kommand.register("sample") {
            then("ping") {
                executes {
                    it.source.sender.sendMessage(text("pong!"))
                }
            }
        }
    }
}