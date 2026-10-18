package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

public class SPECIALScoreboard {
    public static SPECIALScoreboard inst = new SPECIALScoreboard();

    public final String STRENGTH_BOARD = "Strength";
    public final String PERCEPTION_BOARD = "Perception";
    public final String ENDURANCE_BOARD = "Endurance";
    public final String ENDURANCE_BONUS_BOARD = "Endurance_Bonus";
    public final String INTELLIGENCE_BOARD = "Intelligence";
    public final String CHARISMA_BOARD = "Charisma";
    public final String CHARISMA_BONUS_BOARD = "Charisma_Bonus";
    public final String AGILITY_BOARD = "Agility";
    public final String LUCK_BOARD = "Luck";
    public final String LIMIT_BOARD = "Limit";

    private Score strengthScore;
    private Score perceptionScore;
    private Score enduranceScore;
    private Score enduranceBonusScore;
    private Score intelligenceScore;
    private Score charismaScore;
    private Score charismaBonusScore;
    private Score agilityScore;
    private Score luckScore;
    private Score limitScore;
    private int clientStrength;
    private int clientPerception;
    private int clientEndurance;
    private int clientEnduranceBonus;
    private int clientCharisma;
    private int clientCharismaBonus;
    private int clientIntelligence;
    private int clientAgility;
    private int clientLuck;
    private int clientLimit;

    public int getPlayerScorePoints(EntityPlayer player, String boardName) {
        if (player == null || player.getEntityWorld() == null || boardName == null) {
            return 0;
        }

        // 1. get player's current scoreboard
        Scoreboard playerScoreboard = player.getEntityWorld().getScoreboard();

        // 2. start player's board if null
        if (playerScoreboard.getObjective(boardName) == null) {
            playerScoreboard.addScoreObjective(boardName, IScoreCriteria.DUMMY);
        }

        // 3. get the player's objective off the board
        ScoreObjective objective = playerScoreboard.getObjective(boardName);

        // 4. check if he has the objective or not
        if (objective == null) {
            return 0;
        }

        // 5. if limit board, and player doesn't have objective, set it to 10, otherwise, get or create it as normal
        if (boardName.equals(LIMIT_BOARD)) {
            if (!playerScoreboard.entityHasObjective(player.getName(), objective)) {
                limitScore = playerScoreboard.getOrCreateScore(player.getName(), objective);
                limitScore.setScorePoints(10);
            } else {
                limitScore = playerScoreboard.getOrCreateScore(player.getName(), objective);
            }
            return limitScore.getScorePoints();
        }

        // 6. check if player has objective
        if (!playerScoreboard.entityHasObjective(player.getName(), objective)) {
            return 0;
        }

        // 7. get or create the score for the player
        return playerScoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }

    public void setClientScores(int strength, int perception, int endurance, int enduranceBonus, int charisma, int charismaBonus, int intelligence, int agility, int luck, int limit) {
        clientStrength = strength;
        clientPerception = perception;
        clientEndurance = endurance;
        clientEnduranceBonus = enduranceBonus;
        clientCharisma = charisma;
        clientCharismaBonus = charismaBonus;
        clientIntelligence = intelligence;
        clientAgility = agility;
        clientLuck = luck;
        clientLimit = limit;
    }

    public int getClientStrength() {
        return clientStrength;
    }

    public int getClientPerception() {
        return clientPerception;
    }

    public int getClientEndurance() {
        return clientEndurance;
    }

    public int getClientEnduranceBonus() {
        return clientEnduranceBonus;
    }

    public int getClientCharisma() {
        return clientCharisma;
    }

    public int getClientCharismaBonus() {
        return clientCharismaBonus;
    }

    public int getClientIntelligence() {
        return clientIntelligence;
    }

    public int getClientAgility() {
        return clientAgility;
    }

    public int getClientLuck() {
        return clientLuck;
    }

    public int getClientLimit() {
        return clientLimit;
    }
}
