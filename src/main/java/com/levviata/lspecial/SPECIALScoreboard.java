package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

public class SPECIALScoreboard {
    public static SPECIALScoreboard inst = new SPECIALScoreboard();

    public Scoreboard scoreboard;

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

    public void startScores(EntityPlayer player) {
        ScoreObjective objStrength = scoreboard.getObjective(STRENGTH_BOARD);
        ScoreObjective objPerception = scoreboard.getObjective(PERCEPTION_BOARD);
        ScoreObjective objEndurance = scoreboard.getObjective(ENDURANCE_BOARD);
        ScoreObjective objEnduranceBonus = scoreboard.getObjective(ENDURANCE_BONUS_BOARD);
        ScoreObjective objCharisma = scoreboard.getObjective(CHARISMA_BOARD);
        ScoreObjective objCharismaBonus = scoreboard.getObjective(CHARISMA_BONUS_BOARD);
        ScoreObjective objIntelligence = scoreboard.getObjective(INTELLIGENCE_BOARD);
        ScoreObjective objAgility = scoreboard.getObjective(AGILITY_BOARD);
        ScoreObjective objLuck = scoreboard.getObjective(LUCK_BOARD);
        ScoreObjective objLimit = scoreboard.getObjective(LIMIT_BOARD);

        strengthScore = scoreboard.getOrCreateScore(player.getName(), objStrength);
        perceptionScore = scoreboard.getOrCreateScore(player.getName(), objPerception);
        enduranceScore = scoreboard.getOrCreateScore(player.getName(), objEndurance);
        enduranceBonusScore = scoreboard.getOrCreateScore(player.getName(), objEnduranceBonus);
        charismaScore = scoreboard.getOrCreateScore(player.getName(), objCharisma);
        charismaBonusScore = scoreboard.getOrCreateScore(player.getName(), objCharismaBonus);
        intelligenceScore = scoreboard.getOrCreateScore(player.getName(), objIntelligence);
        agilityScore = scoreboard.getOrCreateScore(player.getName(), objAgility);
        luckScore = scoreboard.getOrCreateScore(player.getName(), objLuck);
        if (!scoreboard.entityHasObjective(player.getName(), objLimit)) {
            limitScore = scoreboard.getOrCreateScore(player.getName(), objLimit);
            limitScore.setScorePoints(10);
        } else {
            limitScore = scoreboard.getOrCreateScore(player.getName(), objLimit);
        }
    }

    /** reads a score for the supplied player without relying on the singleton's cached player. */
    public int getPlayerScorePoints(EntityPlayer player, String objectiveName) {
        if (player == null || player.getEntityWorld() == null || objectiveName == null) return 0;

        Scoreboard playerScoreboard = player.getEntityWorld().getScoreboard();
        ScoreObjective objective = playerScoreboard.getObjective(objectiveName);
        if (objective == null || !playerScoreboard.entityHasObjective(player.getName(), objective)) return 0;

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

    public Score getStrengthScore() {
        return strengthScore;
    }

    public Score getPerceptionScore() {
        return perceptionScore;
    }

    public Score getEnduranceScore() {
        return enduranceScore;
    }

    public Score getEnduranceBonusScore() {
        return enduranceBonusScore;
    }

    public Score getIntelligenceScore() {
        return intelligenceScore;
    }

    public Score getCharismaScore() {
        return charismaScore;
    }

    public Score getCharismaBonusScore() {
        return charismaBonusScore;
    }

    public Score getAgilityScore() {
        return agilityScore;
    }

    public Score getLuckScore() {
        return luckScore;
    }

    public Score getLimitScore() {
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
