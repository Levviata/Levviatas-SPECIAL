package com.levviata.lspecial;

import com.levviata.lspecial.command.CommandSetFlySpeed;
import com.levviata.lspecial.command.CommandSetSpeed;
import com.levviata.lspecial.entity.EntityGrappleChain;
import com.levviata.lspecial.network.RequestSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsSetter;
import com.levviata.lspecial.potion.PotionAmplifiedRegeneration;
import com.levviata.lspecial.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
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

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, guiFactory = "com.levviata.lspecial.LSPECIALGuiFactory")
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
        NETWORK.registerMessage(RequestSPECIALStatsPacket.Handler.class, RequestSPECIALStatsPacket.class, 1, Side.SERVER);
        NETWORK.registerMessage(GrappleCameraPacket.Handler.class, GrappleCameraPacket.class, 2, Side.CLIENT);
        NETWORK.registerMessage(GrappleCameraPacket.Handler.class, GrappleCameraPacket.class, 3, Side.SERVER);
        MinecraftForge.EVENT_BUS.register(new GrappleController());
        MinecraftForge.EVENT_BUS.register(new RocketJumpPhysicsHandler());
        MinecraftForge.EVENT_BUS.register(new TF2FlamethrowerHandler());

        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        StatsLogic.init();
        AMATRailgun.init();
        TF2FlamethrowerHandler.initFlameTuning();
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (LSPECIALConfig.setSpeedCommand) event.registerServerCommand(new CommandSetSpeed());
        if (LSPECIALConfig.setFlySpeedCommand) event.registerServerCommand(new CommandSetFlySpeed());
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
/*

    public static String appendModID(String value) {
        return Tags.MOD_ID + ":" + value;
    }*/
}
