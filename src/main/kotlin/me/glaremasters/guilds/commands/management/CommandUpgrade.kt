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
import me.glaremasters.guilds.Guilds
import me.glaremasters.guilds.actions.ActionHandler
import me.glaremasters.guilds.actions.ConfirmAction
import me.glaremasters.guilds.api.events.GuildUpgradeEvent
import me.glaremasters.guilds.configuration.sections.PluginSettings
import me.glaremasters.guilds.configuration.sections.TierSettings
import me.glaremasters.guilds.exceptions.ExpectationNotMet
import me.glaremasters.guilds.guild.Guild
import me.glaremasters.guilds.guild.GuildHandler
import me.glaremasters.guilds.messages.Messages
import me.glaremasters.guilds.utils.Constants
import me.glaremasters.guilds.utils.EconomyUtils
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandAlias("%guilds")
internal class CommandUpgrade : BaseCommand() {
    @Dependency
    lateinit var guilds: Guilds
    @Dependency
    lateinit var guildHandler: GuildHandler
    @Dependency
    lateinit var actionHandler: ActionHandler
    @Dependency
    lateinit var settingsManager: SettingsManager
    @Dependency
    lateinit var permission: Permission

    @Subcommand("upgrade")
    @Description("{@@descriptions.upgrade}")
    @CommandPermission(Constants.BASE_PERM + "upgrade")
    @Syntax("")
    fun upgrade(player: Player, @Conditions("perm:perm=UPGRADE_GUILD") guild: Guild) {
        if (guilds.settingsHandler.mainConf.getProperty(PluginSettings.READ_ONLY)) return
        if (guildHandler.isMaxTier(guild)) {
            throw ExpectationNotMet(Messages.UPGRADE__TIER_MAX)
        }

        val tier = guildHandler.getGuildTier(guild.tier.level + 1)!!
        val cost = tier.cost
        val prosperity = tier.prosperity

        if (guildHandler.memberCheck(guild)) {
            throw ExpectationNotMet(Messages.UPGRADE__NOT_ENOUGH_MEMBERS, "{amount}", guild.tier.membersToRankup.toString())
        }

        if (guild.prosperity < prosperity) {
            throw ExpectationNotMet(Messages.UPGRADE__NOT_ENOUGH_PROSPERITY, "{needed}", (prosperity - guild.prosperity).toString())
        }

        if (!EconomyUtils.hasEnough(guild.balance, cost)) {
            throw ExpectationNotMet(Messages.UPGRADE__NOT_ENOUGH_MONEY, "{needed}", EconomyUtils.format(cost - guild.balance))
        }

        currentCommandIssuer.sendInfo(Messages.UPGRADE__MONEY_WARNING, "{amount}", EconomyUtils.format(cost))
        actionHandler.addAction(player, object : ConfirmAction {
            override fun accept() {
                if (!EconomyUtils.hasEnough(guild.balance, cost)) {
                    throw ExpectationNotMet(Messages.UPGRADE__NOT_ENOUGH_MONEY, "{needed}", EconomyUtils.format(cost - guild.balance))
                }

                guild.balance = guild.balance - cost

                guildHandler.removeGuildPermsFromAll(permission, guild)
                guildHandler.upgradeTier(guild)
                guildHandler.addGuildPermsToAll(permission, guild)
                currentCommandIssuer.sendInfo(Messages.UPGRADE__SUCCESS)


                val event = GuildUpgradeEvent(player, guild, guild.tier)
                Bukkit.getPluginManager().callEvent(event)

                actionHandler.removeAction(player)
            }

            override fun decline() {
                currentCommandIssuer.sendInfo(Messages.UPGRADE__CANCEL)
                actionHandler.removeAction(player)
            }
        })
    }
}
