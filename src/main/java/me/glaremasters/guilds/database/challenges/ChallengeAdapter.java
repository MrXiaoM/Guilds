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
package me.glaremasters.guilds.database.challenges;

import ch.jalu.configme.SettingsManager;
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.configuration.sections.PluginSettings;
import me.glaremasters.guilds.database.DatabaseAdapter;
import me.glaremasters.guilds.database.DatabaseBackend;
import me.glaremasters.guilds.database.challenges.provider.ChallengeJsonProvider;
import me.glaremasters.guilds.guild.GuildChallenge;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ChallengeAdapter {
    private final ChallengeProvider provider;
    private String sqlTablePrefix;
    private SettingsManager settingsManager;

    public ChallengeAdapter(Guilds guilds, DatabaseAdapter adapter) {
        DatabaseBackend backend = adapter.getBackend();
        settingsManager = guilds.getSettingsHandler().getMainConf();
        switch (backend) {
            default:
            case JSON:
                File fileDataFolder = new File(guilds.getDataFolder(), "challenges");
                provider = new ChallengeJsonProvider(fileDataFolder);
                break;
            case MYSQL:
            case SQLITE:
            case MARIADB:
                sqlTablePrefix = adapter.getSqlTablePrefix();
                provider = adapter.getDatabaseManager().getJdbi().onDemand(backend.getChallengeProvider());
                break;
        }
    }

   public void createContainer() throws IOException {
        provider.createContainer(sqlTablePrefix);
   }

    public boolean challengeExists(@NotNull String id) throws IOException {
        return provider.challengeExists(sqlTablePrefix, id);
    }

   public Set<GuildChallenge> getAllChallenges() throws IOException {
        return provider.getAllChallenges(sqlTablePrefix);
   }

   public GuildChallenge getChallenge(@NotNull String id) throws IOException {
        return provider.getChallenge(sqlTablePrefix, id);
   }

   public void saveChallenges(@NotNull Set<GuildChallenge> challenges) throws IOException {
       if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        for (GuildChallenge challenge : challenges) {
            saveChallenge(challenge);
        }
   }

   public void saveChallenge(@NotNull GuildChallenge challenge) throws IOException {
       if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
       if (!challengeExists(challenge.getId().toString())) {
           createChallenge(challenge);
       } else {
           updateChallenge(challenge);
       }
   }

   public void createChallenge(@NotNull GuildChallenge challenge) throws IOException {
       if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        provider.createChallenge(sqlTablePrefix, challenge.getId().toString(), Guilds.getGson().toJson(challenge, GuildChallenge.class));
   }

    public void updateChallenge(@NotNull GuildChallenge challenge) throws IOException {
        if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        provider.updateChallenge(sqlTablePrefix, challenge.getId().toString(), Guilds.getGson().toJson(challenge, GuildChallenge.class));
    }

   public void deleteChallenge(@NotNull String id) throws IOException {
       if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        provider.deleteChallenge(sqlTablePrefix, id);
   }
}
