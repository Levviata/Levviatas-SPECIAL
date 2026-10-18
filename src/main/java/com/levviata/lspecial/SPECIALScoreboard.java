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

    public void startBoards() {
        if (scoreboard.getObjective(STRENGTH_BOARD) == null) {
            scoreboard.addScoreObjective(STRENGTH_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(PERCEPTION_BOARD) == null) {
            scoreboard.addScoreObjective(PERCEPTION_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(ENDURANCE_BOARD) == null) {
            scoreboard.addScoreObjective(ENDURANCE_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(ENDURANCE_BONUS_BOARD) == null) {
            scoreboard.addScoreObjective(ENDURANCE_BONUS_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(CHARISMA_BOARD) == null) {
            scoreboard.addScoreObjective(CHARISMA_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(CHARISMA_BONUS_BOARD) == null) {
            scoreboard.addScoreObjective(CHARISMA_BONUS_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(INTELLIGENCE_BOARD) == null) {
            scoreboard.addScoreObjective(INTELLIGENCE_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(AGILITY_BOARD) == null) {
            scoreboard.addScoreObjective(AGILITY_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(LUCK_BOARD) == null) {
            scoreboard.addScoreObjective(LUCK_BOARD, IScoreCriteria.DUMMY);
        }
        if (scoreboard.getObjective(LIMIT_BOARD) == null) {
            scoreboard.addScoreObjective(LIMIT_BOARD, IScoreCriteria.DUMMY);
        }
    }

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
        if (objective == null || !playerScoreboard.entityHasObjective(player.getName(), objective)) {
            return 0;
        }

        // 5. if limit board, and player doesn't have it, set it to 10, otherwise, get or create it as normal
        if (boardName.equals(LIMIT_BOARD)) {
            if (!playerScoreboard.entityHasObjective(player.getName(), objective)) {
                limitScore = playerScoreboard.getOrCreateScore(player.getName(), objective);
                limitScore.setScorePoints(10);
            } else {
                limitScore = playerScoreboard.getOrCreateScore(player.getName(), objective);
            }
            return limitScore.getScorePoints();
        }

        // 5. get or create the score for the player
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

    public Score getStrengthScore(EntityPlayer player) {
        ScoreObjective objStrength = scoreboard.getObjective(STRENGTH_BOARD);
        strengthScore = scoreboard.getOrCreateScore(player.getName(), objStrength);
        return strengthScore;
    }

    public Score getPerceptionScore(EntityPlayer player) {
        ScoreObjective objPerception = scoreboard.getObjective(PERCEPTION_BOARD);
        perceptionScore = scoreboard.getOrCreateScore(player.getName(), objPerception);
        return perceptionScore;
    }

    public Score getEnduranceScore(EntityPlayer player) {
        ScoreObjective objEndurance = scoreboard.getObjective(ENDURANCE_BOARD);
        enduranceScore = scoreboard.getOrCreateScore(player.getName(), objEndurance);
        return enduranceScore;
    }

    public Score getEnduranceBonusScore(EntityPlayer player) {
        ScoreObjective objEnduranceBonus = scoreboard.getObjective(ENDURANCE_BONUS_BOARD);
        enduranceBonusScore = scoreboard.getOrCreateScore(player.getName(), objEnduranceBonus);
        return enduranceBonusScore;
    }
    public Score getCharismaScore(EntityPlayer player) {
        ScoreObjective objCharisma = scoreboard.getObjective(CHARISMA_BOARD);
        charismaScore = scoreboard.getOrCreateScore(player.getName(), objCharisma);
        return charismaScore;
    }
    public Score getCharismaBonusScore(EntityPlayer player) {
        ScoreObjective objCharismaBonus = scoreboard.getObjective(CHARISMA_BONUS_BOARD);
        charismaBonusScore = scoreboard.getOrCreateScore(player.getName(), objCharismaBonus);
        return charismaBonusScore;
    }
    
    public Score getIntelligenceScore(EntityPlayer player) {
        ScoreObjective objIntelligence = scoreboard.getObjective(INTELLIGENCE_BOARD);
        intelligenceScore = scoreboard.getOrCreateScore(player.getName(), objIntelligence);
        return intelligenceScore;
    }

    public Score getAgilityScore(EntityPlayer player) {
        ScoreObjective objAgility = scoreboard.getObjective(AGILITY_BOARD);
        agilityScore = scoreboard.getOrCreateScore(player.getName(), objAgility);
        return agilityScore;
    }

    public Score getLuckScore(EntityPlayer player) {
        ScoreObjective objLuck = scoreboard.getObjective(LUCK_BOARD);
        luckScore = scoreboard.getOrCreateScore(player.getName(), objLuck);
        return luckScore;
    }

    public Score getLimitScore(EntityPlayer player) {


        return limitScore;
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
