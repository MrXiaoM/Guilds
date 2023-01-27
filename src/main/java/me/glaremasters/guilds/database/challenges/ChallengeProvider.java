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

import me.glaremasters.guilds.guild.GuildChallenge;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ChallengeProvider {

    /**
     * Creates the container that will hold challenges
     * @param tablePrefix the prefix, if any, to use
     */
    void createContainer(@Nullable String tablePrefix) throws IOException;

    /**
     * Gets all challenges from the database
     * @param tablePrefix the prefix, if any, to use
     * @return a list of challenges
     */
    Set<GuildChallenge> getAllChallenges(@Nullable String tablePrefix) throws IOException;

    /**
     * Checks whether or not a guild with the specified id exists
     * @param id the guild id
     * @return true or false
     */
    boolean challengeExists(@Nullable String tablePrefix, @NotNull String id) throws IOException;

    /**
     * Gets a single challenge by id
     * @param tablePrefix the prefix, if any, to use
     * @param id the id of the challenge to load
     * @return the found challenge or null
     */
    GuildChallenge getChallenge(@Nullable String tablePrefix, @NotNull String id) throws IOException;

    /**
     * Saves a new challenge to the database
     * @param tablePrefix the prefix, if any, to use
     * @param id the id of the new challenge
     * @param data the data of the new challenge
     */
    void createChallenge(@Nullable String tablePrefix, String id, String data) throws  IOException;

    /**
     * Updates a challenge in the database
     * @param tablePrefix the prefix, if any, to use
     * @param id the id of the challenge to update
     * @param data the updated data of the challenge
     */
    void updateChallenge(@Nullable String tablePrefix, @NotNull String id, @NotNull String data) throws IOException;

    /**
     * Deletes a challenge from the database
     * @param tablePrefix the prefix, if any, to use
     * @param id the challenge id to delete
     */
    void deleteChallenge(@Nullable String tablePrefix, @NotNull String id) throws IOException;

}
