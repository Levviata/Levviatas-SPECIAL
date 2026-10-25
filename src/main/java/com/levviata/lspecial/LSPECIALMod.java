package com.levviata.lspecial;

import com.levviata.lspecial.command.CommandSetFlySpeed;
import com.levviata.lspecial.command.CommandSetSpeed;
import com.levviata.lspecial.entity.EntityGrappleChain;
import com.levviata.lspecial.network.RequestSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncConfigPacket;
import com.levviata.lspecial.network.SyncConfigSetter;
import com.levviata.lspecial.network.SyncSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsSetter;
import com.levviata.lspecial.network.SyncSPECIALGunStatePacket;
import com.levviata.lspecial.network.SyncSPECIALGunStateSetter;
import com.levviata.lspecial.potion.PotionAmplifiedRegeneration;
import com.levviata.lspecial.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static com.levviata.lspecial.SPECIALScoreboard.inst;
import static com.levviata.lspecial.potion.PotionAmplifiedRegeneration.AMPLIFIED_REGENERATION_NAME;

// AI IS FREE SLAVERY
/*
notes for whoever reads this next
-> no minecraft/forge/ntm jars where the code gets written, only ci compiles it, nothing here was run in game
-> MixinEntityAITargetNeutrality targets EntityAITarget.isSuitableTarget(EntityLiving, EntityLivingBase, boolean, boolean) by descriptor, require = 0 so a wrong one is skipped silently, check the log if neutral mobs still lock on
-> AMAT caliber switch: no cause found by reading the code, turn on info logging with the AmmoCycle identifier and send the log if it still fails
-> endurance bonus is a running total now (each score step adds the formula difference), assumed from the report, the persisted tags are LSPECIALEnduranceBonus and LSPECIALEnduranceScore
-> config sync now covers client, perks and messages too, only recipes, commands and logging stay local
-> the carbine auto fire is set by the perk itself, the "every gun goes auto at intelligence 15" part of the revolver perk is still there (revolver auto fire is scheduled for removal)
*/
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, guiFactory = "com.levviata.lspecial.LSPECIALGuiFactory",
     dependencies = "required-after:hbm", acceptedMinecraftVersions = "[1.12.2]")
public class LSPECIALMod {
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static final int HIGHER_PERK = 15;
    public static final int LESSER_PERK = 5;

    public static boolean hasNecessaryLevel(int intelligence, boolean higherOrLesser) {
        if (!LSPECIALConfig.intelligence) {
            return false;
        }

        if (intelligence >= HIGHER_PERK && higherOrLesser) {
            return true;
        }
        if (intelligence >= LESSER_PERK && !higherOrLesser) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether the player's Intelligence score meets the given perk level (use LESSER_PERK or HIGHER_PERK).
     */
    public static boolean hasIntelligenceLevel(EntityPlayer player, int level) {
        return LSPECIALConfig.intelligence
                && player != null
                && inst != null
                && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= level;
    }

    public static void info(String identifier, String message, Object... params) {
        if (LSPECIALConfig.loggingInfo && Arrays.stream(LSPECIALConfig.listeningThisInfo.split(","))
                .map(String::trim)
                .anyMatch(identifier::equals)) {
            LOGGER.info("[" + identifier + "] " + message, params);
        }
    }

    public static void logWarn(String message, Object... params) {
        if (LSPECIALConfig.loggingWarn) {
            LOGGER.warn(message, params);
        }
    }

    public static void logError(String message, Object... params) {
        if (LSPECIALConfig.loggingError) {
            LOGGER.error(message, params);
        }
    }

    public static void playerLog(EntityPlayer player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(
                    "[LSPECIAL] " + message
            ));
        }
    }

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    @SidedProxy(clientSide = "com.levviata.lspecial.proxy.ClientProxy", serverSide = "com.levviata.lspecial.proxy.ServerProxy")
    private static CommonProxy proxy;

    /*@mod.Instance
    private static LSPECIALMod inst;*/

    public static final Potion AMPLIFIED_REGENERATION = new PotionAmplifiedRegeneration();

    /**
     * <a href="https://cleanroommc.com/wiki/forge-mod-development/event#overview">
     *     take a look at how many FMLStateEvents you can listen to via the @Mod.EventHandler annotation here
     * </a>
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LSPECIALConfig.init(event.getSuggestedConfigurationFile());
        info("RocketDebug", "rocket diagnostics initialized: infoLogging={} selfDamageProtection={} rocketJumpPerk={} intelligenceEnabled={} requiredIntelligence={}",
                LSPECIALConfig.loggingInfo, LSPECIALConfig.stingerSelfDamageProtection,
                LSPECIALConfig.rocketJumpLesserPerk, LSPECIALConfig.intelligence,
                LSPECIALConfig.rocketJumpLesserPerkLevel);
        EntityRegistry.registerModEntity(new ResourceLocation(Tags.MOD_ID, "grapple_chain"),
                EntityGrappleChain.class, "grapple_chain", 1, this, 64, 1, true);
        MinecraftForge.EVENT_BUS.register(new GrappleChainSpawnHandler());
        MinecraftForge.EVENT_BUS.register(new BobbleheadUse());
        MinecraftForge.EVENT_BUS.register(new StatsLogic());
        MinecraftForge.EVENT_BUS.register(new SPECIALGameplayHandler());
        MinecraftForge.EVENT_BUS.register(new BobbleheadRecipeManager());
        MinecraftForge.EVENT_BUS.register(new LSPECIALContainerOpenHandler());

        AMPLIFIED_REGENERATION.setRegistryName(Tags.MOD_ID, AMPLIFIED_REGENERATION_NAME);

        ForgeRegistries.POTIONS.register(AMPLIFIED_REGENERATION);

        NETWORK.registerMessage(SyncSPECIALStatsSetter.class, SyncSPECIALStatsPacket.class, 0, Side.CLIENT);
        NETWORK.registerMessage(SyncSPECIALGunStateSetter.class, SyncSPECIALGunStatePacket.class, 4, Side.CLIENT);
        NETWORK.registerMessage(RequestSPECIALStatsPacket.Handler.class, RequestSPECIALStatsPacket.class, 1, Side.SERVER);
        // a packet class can only own one discriminator, so both sides share id 2 (the handler branches on context.side)
        NETWORK.registerMessage(GrappleCameraPacket.Handler.class, GrappleCameraPacket.class, 2, Side.CLIENT);
        NETWORK.registerMessage(GrappleCameraPacket.Handler.class, GrappleCameraPacket.class, 2, Side.SERVER);
        NETWORK.registerMessage(SyncConfigSetter.class, SyncConfigPacket.class, 5, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(new GrappleController());
        MinecraftForge.EVENT_BUS.register(new RocketJumpPhysicsHandler());
        MinecraftForge.EVENT_BUS.register(new TF2FlamethrowerHandler());

        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        AMATRailgun.init();
        TF2FlamethrowerHandler.initFlameTuning();
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (LSPECIALConfig.setSpeedCommand) event.registerServerCommand(new CommandSetSpeed());
        if (LSPECIALConfig.setFlySpeedCommand) event.registerServerCommand(new CommandSetFlySpeed());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        // all of this is static and would otherwise outlive the world (singleplayer keeps the JVM), NTM's shared gun objects included
        StatsLogic.restoreAllNativeGuns();
        RefundMagazine.clearSharedRolls();
        GrappleController.clearAll();
        RocketJumpHandler.clearAll();
        RocketDamageHandler.clearAll();
    }

    /**
     * Sends this server's gameplay config to a joining player, so both sides agree on which perks are on.
     */
    public static void syncConfig(EntityPlayerMP player) {
        NETWORK.sendTo(new SyncConfigPacket(LSPECIALConfig.syncSnapshot()), player);
    }

    /**
     * Sends the current config to everybody connected to a running server, the host of an integrated server skips it.
     */
    public static void syncConfigToAll() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            syncConfig(player);
        }
    }

    public static void syncStats(EntityPlayerMP player) {
        NETWORK.sendTo(
                new SyncSPECIALStatsPacket(
                        inst.getPlayerScorePoints(player, inst.STRENGTH_BOARD),
                        inst.getPlayerScorePoints(player, inst.PERCEPTION_BOARD),
                        inst.getPlayerScorePoints(player, inst.ENDURANCE_BOARD),
                        inst.getPlayerScorePoints(player, inst.ENDURANCE_BONUS_BOARD),
                        inst.getPlayerScorePoints(player, inst.CHARISMA_BOARD),
                        inst.getPlayerScorePoints(player, inst.CHARISMA_BONUS_BOARD),
                        inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD),
                        inst.getPlayerScorePoints(player, inst.AGILITY_BOARD),
                        inst.getPlayerScorePoints(player, inst.LUCK_BOARD),
                        inst.getPlayerScorePoints(player, inst.LIMIT_BOARD)
                ),
                player
        );
    }

    public static void syncGunState(EntityPlayerMP player, StatsLogic.PlayerState state) {
        if (player == null || state == null) {
            return;
        }

        int size = state.gunStates.size();
        int[] indices = new int[size];
        float[] damages = new float[size];

        for (int i = 0; i < size; i++) {
            StatsLogic.GunStateView gunState = state.getGunStateView(i);
            indices[i] = gunState.index;
            damages[i] = gunState.damage;
        }

        LSPECIALMod.info("GunDebug",
                "Server sending gun state: player={}, gun={}, registry={}, meta={}, currentIndex={}, entries={}",
                player.getName(),
                state.modifiedStackRef == null || state.modifiedStackRef.isEmpty() ? "EMPTY" : state.modifiedStackRef.getDisplayName(),
                state.modifiedStackRef == null || state.modifiedStackRef.isEmpty() ? "null" : state.modifiedStackRef.getItem().getRegistryName(),
                state.modifiedStackRef == null || state.modifiedStackRef.isEmpty() ? -1 : state.modifiedStackRef.getMetadata(),
                state.currentGunIndex,
                size);

        for (int i = 0; i < size; i++) {
            LSPECIALMod.info("GunDebug", "Server sync entry: index={}, damage={}", indices[i], damages[i]);
        }

        NETWORK.sendTo(
                new SyncSPECIALGunStatePacket(
                        state.modifiedStackRef == null ? ItemStack.EMPTY : state.modifiedStackRef.copy(),
                        state.currentGunIndex,
                        indices,
                        damages
                ),
                player
        );
    }
/*

    public static String appendModID(String value) {
        return Tags.MOD_ID + ":" + value;
    }*/
}
