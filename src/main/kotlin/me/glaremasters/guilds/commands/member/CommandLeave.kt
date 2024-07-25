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
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Dependency
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.protection.FlagPermissions
import java.util.concurrent.TimeUnit
import me.glaremasters.guilds.Guilds
import me.glaremasters.guilds.actions.ActionHandler
import me.glaremasters.guilds.actions.ConfirmAction
import me.glaremasters.guilds.api.events.GuildLeaveEvent
import me.glaremasters.guilds.api.events.GuildRemoveEvent
import me.glaremasters.guilds.configuration.sections.CooldownSettings
import me.glaremasters.guilds.cooldowns.Cooldown
import me.glaremasters.guilds.cooldowns.CooldownHandler
import me.glaremasters.guilds.guild.Guild
import me.glaremasters.guilds.guild.GuildHandler
import me.glaremasters.guilds.messages.Messages
import me.glaremasters.guilds.utils.ClaimUtils
import me.glaremasters.guilds.utils.Constants
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.codemc.worldguardwrapper.WorldGuardWrapper

@CommandAlias("%guilds")
internal class CommandLeave : BaseCommand() {
    @Dependency
    lateinit var guilds: Guilds
    @Dependency
    lateinit var guildHandler: GuildHandler
    @Dependency
    lateinit var actionHandler: ActionHandler
    @Dependency
    lateinit var permission: Permission
    @Dependency
    lateinit var settingsManager: SettingsManager
    @Dependency
    lateinit var cooldownHandler: CooldownHandler

    @Subcommand("leave|exit")
    @Description("{@@descriptions.leave}")
    @Syntax("")
    @CommandPermission(Constants.BASE_PERM + "leave")
    @Conditions("NotMigrating")
    fun leave(player: Player, guild: Guild) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        if (guild.isMaster(player)) currentCommandIssuer.sendInfo(Messages.LEAVE__WARNING_GUILDMASTER) else currentCommandIssuer.sendInfo(Messages.LEAVE__WARNING)

        val cooldownName = Cooldown.Type.Join.name
        val cooldownTime = settingsManager.getProperty(CooldownSettings.JOIN)
        val name = player.name

        actionHandler.addAction(player, object : ConfirmAction {
            override fun accept() {
                val event = GuildLeaveEvent(player, guild)
                Bukkit.getPluginManager().callEvent(event)

                if (event.isCancelled) {
                    return
                }

                if (guild.isMaster(player)) {
                    val removeEvent = GuildRemoveEvent(player, guild, GuildRemoveEvent.Cause.MASTER_LEFT)
                    Bukkit.getPluginManager().callEvent(removeEvent)

                    if (removeEvent.isCancelled) {
                        return
                    }

                    guild.sendMessage(currentCommandManager, Messages.LEAVE__GUILDMASTER_LEFT, "{player}", name)
                    currentCommandIssuer.sendInfo(Messages.LEAVE__SUCCESSFUL)
                    guildHandler.removeGuildPermsFromAll(permission, guild)
                    guildHandler.removeRolePermsFromAll(permission, guild)
                    guildHandler.removeAlliesOnDelete(guild)
                    guildHandler.notifyAllies(guild, guilds.commandManager)
                    cooldownHandler.addCooldown(player, cooldownName, cooldownTime, TimeUnit.SECONDS)
                    ClaimUtils.deleteWithGuild(guild, settingsManager)
                    guildHandler.removeGuild(guild)
                } else {
                    guildHandler.removeGuildPerms(permission, player)
                    guildHandler.removeRolePerm(permission, player)
                    cooldownHandler.addCooldown(player, cooldownName, cooldownTime, TimeUnit.SECONDS)

                    if (ClaimUtils.isEnabled(settingsManager)) {
                        val wrapper = WorldGuardWrapper.getInstance()
                        ClaimUtils.getGuildClaim(wrapper, player, guild).ifPresent { region -> ClaimUtils.removeMember(region, player) }
                    }

                    guild.removeMember(player)
                    guildHandler.removeFromMemberCache(player.uniqueId)
                    currentCommandIssuer.sendInfo(Messages.LEAVE__SUCCESSFUL)
                    guild.memberLeaveOrKickClean(player.name)
                    guild.sendMessage(currentCommandManager, Messages.LEAVE__PLAYER_LEFT, "{player}", name)
                }
                actionHandler.removeAction(player)
            }

            override fun decline() {
                currentCommandIssuer.sendInfo(Messages.LEAVE__CANCELLED)
                actionHandler.removeAction(player)
            }
        })
    }
}
fun Guild.memberLeaveOrKickClean(player: String) {
    residence?.takeIf { it.isNotEmpty() }?.also { resName ->
        val res = Residence.getInstance().residenceManagerAPI.getByName(resName)
        val owner = guildMaster.name
        if (res != null && owner != null) {
            if (res.permissions.playerHas(owner, com.bekvon.bukkit.residence.containers.Flags.admin, res.isOwner(owner))) {
                res.permissions.removeAllPlayerFlags(player)
                Guilds.getInstance().logger.info("玩家 $player 离开公会 $name，已移除其在 $residence 领地的权限")
            } else {
                Guilds.getInstance().logger.warning("玩家 $player 离开公会 $name，但公会长没有领地 $residence 的权限，无法移除权限")
            }
        }
    }
}