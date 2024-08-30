package me.lrxh.practice.util;

import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import me.lrxh.practice.Practice;
import me.lrxh.practice.match.Match;
import me.lrxh.practice.match.impl.BasicFreeForAllMatch;
import me.lrxh.practice.match.impl.BasicTeamMatch;
import me.lrxh.practice.match.participant.MatchGamePlayer;
import me.lrxh.practice.participant.GameParticipant;
import me.lrxh.practice.profile.Profile;
import me.lrxh.practice.profile.ProfileState;
import me.lrxh.practice.queue.QueueProfile;
import me.lrxh.practice.util.config.BasicConfigurationFile;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public final class PlaceholderUtil {

    /**
     * Format the lines and replace the placeholders
     *
     * @param lines  the lines
     * @param player the player
     * @return the formatted lines
     */
    public List<String> format(List<String> lines, Player player) {
        List<String> formattedLines = new ArrayList<>();
        Profile profile = Profile.getByUuid(player.getUniqueId());
        QueueProfile queueProfile = profile.getQueueProfile();
        BasicConfigurationFile scoreboardConfig = Practice.getInstance().getScoreboardConfig();

        for (String line : lines) {
            line = line.replaceAll("<online>", String.valueOf(Bukkit.getServer().getOnlinePlayers().size()));
            line = line.replaceAll("<fighting>", String.valueOf(Practice.getInstance().getCache().getMatches().size() * 2));
            line = line.replaceAll("<queueing>", String.valueOf(Practice.getInstance().getCache().getPlayers().size()));
            line = line.replaceAll("<your_ping>", String.valueOf((BukkitReflection.getPing(player))));
            line = line.replaceAll("<player>", player.getName());
            line = line.replaceAll("<wins>", String.valueOf(profile.getWins()));
            line = line.replaceAll("<losses>", String.valueOf(profile.getLosses()));
            line = line.replaceAll("<elo>", String.valueOf(profile.getElo()));
            line = line.replaceAll("<theme>", CC.translate("&" + profile.getOptions().theme().getColor().getChar()));

            if (line.contains("<silent>") && !profile.isSilent()) {
                continue;
            } else {
                line = line.replaceAll("<silent>", "");
            }
            if (line.contains("<follow>") && profile.getFollowing().isEmpty()) {
                continue;
            } else {
                line = line.replaceAll("<follow>", "");
            }

            if (!profile.getFollowing().isEmpty()) {
                line = line.replaceAll("<followedPlayer>", Bukkit.getPlayer(profile.getFollowing().get(0)).getName());
            } else {
                line = line.replaceAll("<followedPlayer>", "");
            }

            if (profile.getState() == ProfileState.QUEUEING) {
                line = line.replaceAll("<kit>", queueProfile.getQueue().getKit().getName());
                line = line.replaceAll("<type>", queueProfile.getQueue().isRanked() ? "Ranked" : "Unranked");
                line = line.replaceAll("<time>", TimeUtil.millisToTimer(queueProfile.getPassed()));
                line = line.replaceAll("<minElo>", String.valueOf(queueProfile.getMinRange()));
                line = line.replaceAll("<maxElo>", String.valueOf(queueProfile.getMaxRange()));
            }

            if (profile.getParty() != null) {
                line = line.replaceAll("<leader>", profile.getParty().getLeader().getName());
                line = line.replaceAll("<party-size>", String.valueOf(profile.getParty().getListOfPlayers().size()));
            }
            Match match = profile.getMatch();
            if (match != null) {
                if (match instanceof BasicTeamMatch) {
                    GameParticipant<MatchGamePlayer> participantA = match.getParticipantA();
                    GameParticipant<MatchGamePlayer> participantB = match.getParticipantB();

                    boolean aTeam = match.getParticipantA().containsPlayer(player.getUniqueId());
                    GameParticipant<MatchGamePlayer> playerTeam = aTeam ? participantA : participantB;
                    GameParticipant<MatchGamePlayer> opponentTeam = aTeam ? participantB : participantA;

                    line = line.replaceAll("<opponentsCount>", String.valueOf(opponentTeam.getAliveCount()))
                            .replaceAll("<opponentsMax>", String.valueOf(opponentTeam.getPlayers().size()))
                            .replaceAll("<teamCount>", String.valueOf(playerTeam.getAliveCount()))
                            .replaceAll("<teamMax>", String.valueOf(playerTeam.getPlayers().size()));
                }

                if (match instanceof BasicFreeForAllMatch) {
                    BasicFreeForAllMatch basicFreeForAllMatch = (BasicFreeForAllMatch) match;
                    line = line.replaceAll("<remaning>", String.valueOf(basicFreeForAllMatch.getRemainingTeams()));
                }

                if (match.getOpponent(player.getUniqueId()) != null) {
                    line = line.replaceAll("<opponent>", match.getOpponent(player.getUniqueId()).getName());
                    line = line.replaceAll("<duration>", match.getDuration());
                    line = line.replaceAll("<opponent_ping>", String.valueOf(BukkitReflection.getPing(match.getOpponent(player.getUniqueId()))));
                    line = line.replaceAll("<your_hits>", String.valueOf(match.getGamePlayer(player).getHits()));
                    line = line.replaceAll("<opponent_hits>", String.valueOf(match.getGamePlayer(match.getOpponent(player.getUniqueId())).getHits()));
                    line = line.replaceAll("<hit_difference>", getDifference(player, profile));
                    line = line.replaceAll("<combo>", getHitCombo(player, profile));
                    line = line.replaceAll("<another_combo>", getAnotherHitCombo(player, profile));

                    if (match.getKit().getGameRules().isBedfight()) {
                        line = line.replaceAll("<bedA>", match.isBedABroken() ? scoreboardConfig.getString("MATCH.IN-MATCH.BEDFIGHT.BED-BROKEN") : scoreboardConfig.getString("MATCH.IN-MATCH.BEDFIGHT.BED-ALIVE"));
                        line = line.replaceAll("<bedB>", match.isBedBBroken() ? scoreboardConfig.getString("MATCH.IN-MATCH.BEDFIGHT.BED-BROKEN") : scoreboardConfig.getString("MATCH.IN-MATCH.BEDFIGHT.BED-ALIVE"));

                        boolean aTeam = match.getParticipantA().containsPlayer(player.getUniqueId());
                        line = line.replaceAll("<youA>", aTeam ? "" : "YOU");
                        line = line.replaceAll("<youB>", !aTeam ? "" : "YOU");
                    }
                }

                if (profile.getState() == ProfileState.SPECTATING) {
                    line = line.replaceAll("<duration>", match.getDuration());
                }
            }

            if (Practice.getInstance().isPlaceholder()) {
                formattedLines.add(PlaceholderAPI.setPlaceholders(player, line));
            } else {
                formattedLines.add(line);
            }
        }
        return formattedLines;
    }

    /**
     * Get the difference between the player and the opponent hits
     *
     * @param player the player
     * @return the difference between the player and the opponent hits
     */
    public String getDifference(Player player, Profile profile) {
        BasicConfigurationFile scoreboardConfig = Practice.getInstance().getScoreboardConfig();
        Match match = profile.getMatch();

        Integer playerHits = match.getGamePlayer(player).getHits();
        Integer opponentHits = match.getGamePlayer(match.getOpponent(player.getUniqueId())).getHits();

        String isAdvantage = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.HIT-DIFFERENCE.ADVANTAGE");
        String isTie = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.HIT-DIFFERENCE.TIE");
        String isDisadvantage = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.HIT-DIFFERENCE.DISADVANTAGE");

        isAdvantage = isAdvantage.replace("<advantage>", Integer.toString(playerHits - opponentHits));
        isDisadvantage = isDisadvantage.replace("<disadvantage>", Integer.toString(opponentHits - playerHits));

        if (playerHits - opponentHits > 0) {
            return CC.translate(isAdvantage);
        } else if (playerHits - opponentHits < 0) {
            return CC.translate(isDisadvantage);
        } else {
            return CC.translate(isTie);
        }
    }

    /**
     * Get players combo
     *
     * @param player the player
     * @return the hit combo
     */
    public String getHitCombo(Player player, Profile profile) {
        BasicConfigurationFile scoreboardConfig = Practice.getInstance().getScoreboardConfig();
        Match match = profile.getMatch();

        Integer playerCombo = match.getGamePlayer(player).getCombo();
        Integer opponentCombo = match.getGamePlayer(match.getOpponent(player.getUniqueId())).getCombo();

        String isYourCombo = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.COMBO-COUNTER.YOUR-COMBO");
        String isNoCombo = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.COMBO-COUNTER.NO-COMBO");
        String isOpponentCombo = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.COMBO-COUNTER.OPPONENT-COMBO");

        isYourCombo = isAdvantage.replace("<your_combo>", Integer.toString(playerCombo));
        isOpponentCombo = isDisadvantage.replace("<opponent_combo>", Integer.toString(opponentCombo));

        if (playerCombo > 1) {
            return CC.translate(isYourCombo);
        } else if (opponentCombo > 1) {
            return CC.translate(isOpponentCombo);
        } else {
            return CC.translate(isNoCombo)
        }
    }
    /**
     * Get players combo
     *
     * @param player the player
     * @return the hit combo
     *
     * Primarily made to be able to replicate Bolt
     */
    public String getAnotherHitCombo(Player player, Profile profile) {
        BasicConfigurationFile scoreboardConfig = Practice.getInstance().getScoreboardConfig();
        Match match = profile.getMatch();

        Integer playerCombo = match.getGamePlayer(player).getCombo();
        Integer opponentCombo = match.getGamePlayer(match.getOpponent(player.getUniqueId())).getCombo();

        String isYourCombo = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.ANOTHER-COMBO-COUNTER.YOUR-COMBO");
        String isNoCombo = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.ANOTHER-COMBO-COUNTER.NO-COMBO");
        String isOpponentCombo = scoreboardConfig.getString("MATCH.IN-MATCH.BOXING.ANOTHER-COMBO-COUNTER.OPPONENT-COMBO");

        isYourCombo = isAdvantage.replace("<your_combo>", Integer.toString(playerCombo));
        isOpponentCombo = isDisadvantage.replace("<opponent_combo>", Integer.toString(opponentCombo));

        if (playerCombo > 1) {
            return CC.translate(isYourCombo);
        } else if (opponentCombo > 1) {
            return CC.translate(isOpponentCombo);
        } else {
            return CC.translate(isNoCombo)
        }
    }
}
