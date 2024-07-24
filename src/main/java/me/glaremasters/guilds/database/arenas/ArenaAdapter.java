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
package me.glaremasters.guilds.database.arenas;

import ch.jalu.configme.SettingsManager;
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.arena.Arena;
import me.glaremasters.guilds.configuration.SettingsHandler;
import me.glaremasters.guilds.configuration.sections.PluginSettings;
import me.glaremasters.guilds.database.DatabaseAdapter;
import me.glaremasters.guilds.database.DatabaseBackend;
import me.glaremasters.guilds.database.arenas.provider.ArenaJsonProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArenaAdapter {
    private final ArenaProvider provider;
    private String sqlTablePrefix;
    private SettingsManager settingsManager;

    public ArenaAdapter(Guilds guilds, DatabaseAdapter adapter) {
        DatabaseBackend backend = adapter.getBackend();
        settingsManager = guilds.getSettingsHandler().getMainConf();
        switch(backend) {
            default:
            case JSON:
                File fileDataFolder = new File(guilds.getDataFolder(), "arenas");
                provider = new ArenaJsonProvider(fileDataFolder);
                break;
            case MYSQL:
            case SQLITE:
            case MARIADB:
                sqlTablePrefix = adapter.getSqlTablePrefix();
                provider = adapter.getDatabaseManager().getJdbi().onDemand(backend.getArenaProvider());
        }
    }

    public void createContainer() throws IOException {
        provider.createContainer(sqlTablePrefix);
    }

    public boolean arenaExists(@NotNull String id) throws IOException {
        return provider.arenaExists(sqlTablePrefix, id);
    }

    public List<String> getAllArenaIds() throws IOException {
        return provider.getAllArenaIds(sqlTablePrefix);
    }

    public List<Arena> getAllArenas() throws IOException {
        return provider.getAllArenas(sqlTablePrefix);
    }

    public void saveArenas(@NotNull Collection<Arena> arenas) throws IOException {
        if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        List<String> savedIds = new ArrayList<>();

        for (Arena arena : arenas) {
            saveArena(arena);
            savedIds.add(arena.getId().toString());
        }

        for (String arenaId : getAllArenaIds()) {
            boolean keep = savedIds.stream().anyMatch(id -> id.equals(arenaId));
            if (!keep) {
                deleteArena(arenaId);
            }
        }

        savedIds.clear();
    }

    public void saveArena(@NotNull Arena arena) throws IOException {
        if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        if (!arenaExists(arena.getId().toString())) {
            createArena(arena);
        } else {
            updateArena(arena);
        }
    }

    public void createArena(@NotNull Arena arena) throws IOException {
        if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        provider.createArena(sqlTablePrefix, arena.getId().toString(), Guilds.getGson().toJson(arena, Arena.class));
    }

    public void updateArena(@NotNull Arena arena) throws IOException {
        if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        provider.updateArena(sqlTablePrefix, arena.getId().toString(), Guilds.getGson().toJson(arena, Arena.class));
    }

    public void deleteArena(@NotNull String id) throws IOException {
        if (settingsManager.getProperty(PluginSettings.READ_ONLY)) return;
        provider.deleteArena(sqlTablePrefix, id);
    }
}
