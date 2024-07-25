/*
 * MIT License
 *
 * Copyright (c) 2023 Glare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.glaremasters.guilds.commands.management

import ch.jalu.configme.SettingsManager
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Dependency
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.containers.Flags
import com.bekvon.bukkit.residence.containers.lm
import me.glaremasters.guilds.Guilds
import me.glaremasters.guilds.configuration.sections.PluginSettings
import me.glaremasters.guilds.guild.Guild
import me.glaremasters.guilds.guild.GuildHandler
import me.glaremasters.guilds.messages.Messages
import me.glaremasters.guilds.utils.Constants
import org.bukkit.entity.Player

@CommandAlias("%guilds")
internal class CommandResidence : BaseCommand() {
    @Dependency
    lateinit var guilds: Guilds
    @Dependency
    lateinit var guildHandler: GuildHandler
    @Dependency
    lateinit var settingsManager: SettingsManager

    @Subcommand("res set")
    @Description("设置玩家进入公会后给他哪个领地的权限")
    @CommandPermission(Constants.BASE_PERM + "residence")
    @Syntax("%res")
    fun status(player: Player, @Conditions("perm:perm=CHANGE_HOME") guild: Guild, res: String) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        val plugin = Residence.getInstance()
        val residence = plugin.residenceManagerAPI.getByName(res) ?: null
        if (residence == null) {
            lm.Invalid_Residence.sendMessage(player)
            return
        }
        if (!residence.isOwner(player)) {
            lm.Residence_NotOwner.sendMessage(player)
        }
        guild.residence = residence.name
        player.sendMessage("§e已设置领地为 ${guild.residence}")
    }

    @Subcommand("res perm")
    @Description("设置给玩家哪些领地权限，用英文逗号分隔")
    @CommandPermission(Constants.BASE_PERM + "residence")
    @Syntax("%perms")
    fun perm(player: Player, @Conditions("perm:perm=CHANGE_HOME") guild: Guild, perms: String) {
        val flags = perms.replace("，", ",").split(',').mapNotNull {
            Flags.getFlag(it)
        }
        guild.residencePerm.clear()
        guild.residencePerm.addAll(flags.map(Flags::getName))
        player.sendMessage("§e已设置权限为 §b${flags.joinToString("§f, §b") { it.translated }}")
    }
}
