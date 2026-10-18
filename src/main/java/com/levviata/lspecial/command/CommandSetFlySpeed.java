package com.levviata.lspecial.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.util.*;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

public class CommandSetFlySpeed extends CommandBase {

    // each player has their own speed value
    public static final Map<UUID, Integer> playerSpeeds = new HashMap<>();

    @Override
    @Nonnull
    public String getName() {
        return "setflyspeed";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/setflyspeed <speed>. Proportional to your Agility stat. -1 to reset";
    }

    @Override
    @Nonnull
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {

        // players only
        if (!(sender instanceof EntityPlayer player)) {
            throw new CommandException("This command can only be used by players.");
        }

        // get this player's agility score
        int maxSpeed = inst.getPlayerScorePoints(player, inst.AGILITY_BOARD);

        // no argument
        if (args.length != 1) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Usage: /setflyspeed <speed>. -1 to reset"
            ));

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Your fly speed range is "
                            + TextFormatting.WHITE + "0"
                            + TextFormatting.YELLOW + " - "
                            + TextFormatting.WHITE + maxSpeed
                            + TextFormatting.YELLOW + " based on your Agility."
            ));

            return;
        }

        int requestedSpeed;

        try {
            requestedSpeed = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Speed must be a whole number. -1 to reset."
            ));

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Your fly speed range is "
                            + TextFormatting.WHITE + "0"
                            + TextFormatting.YELLOW + " - "
                            + TextFormatting.WHITE + maxSpeed
            ));

            return;
        }

        // clamp the requested value between 0 and the player's agility
        int actualSpeed = Math.max(0, Math.min(requestedSpeed, maxSpeed));

        if (requestedSpeed == -1) {
            actualSpeed = -1;
        }

        // store this player's speed
        playerSpeeds.put(player.getUniqueID(), actualSpeed);

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Your fly speed has been set to "
                        + TextFormatting.WHITE + actualSpeed
                        + TextFormatting.GREEN + "."
        ));

        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "Your allowed range is "
                        + TextFormatting.WHITE + "0"
                        + TextFormatting.GRAY + " - "
                        + TextFormatting.WHITE + maxSpeed
                        + TextFormatting.GRAY + "."
        ));
    }

    @Override
    public boolean checkPermission(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    // helper method for getting a player's speed
    public static int getPlayerFlySpeed(EntityPlayer player) {
        return playerSpeeds.getOrDefault(player.getUniqueID(), -1);
    }
}