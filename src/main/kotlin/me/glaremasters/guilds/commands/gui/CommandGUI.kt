/*
 * MIT License
 *
 * Copyright (c) 2022 Glare
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
package me.glaremasters.guilds.commands.gui

import ch.jalu.configme.SettingsManager
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Dependency
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import dev.triumphteam.gui.guis.PaginatedGui
import me.glaremasters.guilds.Guilds
import me.glaremasters.guilds.challenges.ChallengeHandler
import me.glaremasters.guilds.configuration.sections.PluginSettings
import me.glaremasters.guilds.exceptions.InvalidTierException
import me.glaremasters.guilds.guild.Guild
import me.glaremasters.guilds.guild.GuildHandler
import me.glaremasters.guilds.utils.Constants
import org.apache.logging.log4j.core.config.Property
import org.bukkit.entity.Player

@CommandAlias("%guilds")
internal class CommandGUI : BaseCommand() {
    @Dependency lateinit var guilds: Guilds
    @Dependency lateinit var guildHandler: GuildHandler
    @Dependency lateinit var settingsManager: SettingsManager
    @Dependency lateinit var challengeHandler: ChallengeHandler

    @Subcommand("buff")
    @Description("{@@descriptions.buff}")
    @Syntax("")
    @CommandPermission(Constants.BASE_PERM + "buff")
    fun buff(player: Player, @Conditions("perm:perm=ACTIVATE_BUFF") guild: Guild) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        if (!guild.tier.isUseBuffs) {
            throw InvalidTierException()
        }

        guilds.guiHandler.buffs.get(player, guild, guilds.commandManager).open(player)
    }
/*
    @Subcommand("info")
    @Description("{@@descriptions.info}")
    @Syntax("")
    @CommandPermission(Constants.BASE_PERM + "info")
    fun info(player: Player, guild: Guild) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        guilds.guiHandler.info.get(guild, player).open(player)
    }
*/
    @Subcommand("list")
    @Description("{@@descriptions.list}")
    @Syntax("")
    @CommandPermission(Constants.BASE_PERM + "list")
    fun list(player: Player) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        val chain = Guilds.newChain<Any>()
        chain.async {
            chain.setTaskData("data", guilds.guiHandler.list.get(player))
        } .sync {
            (chain.getTaskData<Any>("data") as PaginatedGui).open(player)
        }.execute()
    }
    @Subcommand("gui")
    @Description("awa")
    @Syntax("%title %command")
    @CommandPermission(Constants.BASE_PERM + "list")
    fun gui(player: Player, title: String, command: String) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        val chain = Guilds.newChain<Any>()
        chain.async {
            chain.setTaskData("data", guilds.guiHandler.list.get(player, title, command))
        } .sync {
            (chain.getTaskData<Any>("data") as PaginatedGui).open(player)
        }.execute()
    }

    @Subcommand("mgui")
    @Description("awa")
    @Syntax("%title %command")
    @CommandPermission(Constants.BASE_PERM + "members")
    fun mgui(player: Player, guild: Guild, title: String, command: String) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        guilds.guiHandler.members.get(guild, player).open(player)
    }

    @Subcommand("members")
    @Description("{@@descriptions.members}")
    @Syntax("")
    @CommandPermission(Constants.BASE_PERM + "members")
    fun members(player: Player, guild: Guild) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        guilds.guiHandler.members.get(guild, player).open(player)
    }
/*
    @Subcommand("vaults")
    @Description("{@@descriptions.vault}")
    @Syntax("")
    @CommandPermission(Constants.BASE_PERM + "vault")
    fun vaults(player: Player, @Conditions("perm:perm=OPEN_VAULT") guild: Guild) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        //guilds.guiHandler.vaults.get(guild, player).open(player)
    }
*/

    @Subcommand("vault")
    @Description("{@@descriptions.vault}")
    @Syntax("%amount")
    @CommandPermission(Constants.BASE_PERM + "vault")
    fun vault(player: Player, @Conditions("perm:perm=OPEN_VAULT") guild: Guild, amount: Int) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        //guilds.guiHandler.vaults.get(guild, player).open(player)
        if (amount < 1 || amount > guild.tier.vaultAmount) return
        if ((amount == 2 || amount == 3) && challengeHandler.getChallenge(guild) != null) {
            player.sendMessage("公会战期间禁止操作战利品仓库")
            return
        }
        fun check() {
            try {
                guildHandler.getGuildVault(guild, amount)
            } catch (ex: IndexOutOfBoundsException) {
                guildHandler.vaults[guild]?.add(guildHandler.createNewVault(settingsManager))
                check()
            }
        }
        check()
        player.openInventory(guildHandler.getGuildVault(guild, amount))
        guildHandler.opened.add(player)
    }
}
