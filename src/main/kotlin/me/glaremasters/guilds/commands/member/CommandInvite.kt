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
package me.glaremasters.guilds.commands.member

import ch.jalu.configme.SettingsManager
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Dependency
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Single
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import co.aikar.commands.annotation.Values
import me.glaremasters.guilds.Guilds
import me.glaremasters.guilds.api.events.GuildInviteEvent
import me.glaremasters.guilds.configuration.sections.PluginSettings
import me.glaremasters.guilds.exceptions.ExpectationNotMet
import me.glaremasters.guilds.guild.Guild
import me.glaremasters.guilds.guild.GuildHandler
import me.glaremasters.guilds.messages.Messages
import me.glaremasters.guilds.utils.Constants
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandAlias("%guilds")
internal class CommandInvite : BaseCommand() {
    @Dependency
    lateinit var guilds: Guilds
    @Dependency
    lateinit var guildHandler: GuildHandler
    @Dependency
    lateinit var settingsManager: SettingsManager

    @Subcommand("invite")
    @Description("{@@descriptions.invite}")
    @CommandPermission(Constants.BASE_PERM + "invite")
    @CommandCompletion("@players")
    @Syntax("%name")
    fun invite(player: Player, @Conditions("perm:perm=INVITE") guild: Guild, @Values("@players") @Single target: String) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        val user = Bukkit.getPlayer(target)

        if (user == null || !user.isOnline) {
            throw ExpectationNotMet(Messages.ERROR__PLAYER_NOT_FOUND, "{player}", target)
        }

        if (guildHandler.getGuild(user) != null) {
            throw ExpectationNotMet(Messages.INVITE__ALREADY_IN_GUILD, "{player}", target)
        }

        if (guild.checkIfInvited(user)) {
            throw ExpectationNotMet(Messages.INVITE__ALREADY_INVITED)
        }

        val event = GuildInviteEvent(player, guild, user)
        Bukkit.getPluginManager().callEvent(event)

        if (event.isCancelled) {
            return
        }

        guild.inviteMember(user.uniqueId)
        currentCommandManager.getCommandIssuer(user).sendInfo(Messages.INVITE__MESSAGE, "{player}", player.name, "{guild}", guild.name)
        currentCommandIssuer.sendInfo(Messages.INVITE__SUCCESSFUL, "{player}", user.name)
    }
}
