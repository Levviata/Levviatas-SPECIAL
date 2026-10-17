package com.levviata.lspecial;

import com.levviata.lspecial.command.CommandSetFlySpeed;
import com.levviata.lspecial.command.CommandSetSpeed;
import com.levviata.lspecial.network.RequestSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsPacket;
import com.levviata.lspecial.potion.PotionAmplifiedRegeneration;
import com.levviata.lspecial.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraft.util.ResourceLocation;
import com.levviata.lspecial.entity.EntityGrappleChain;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.levviata.lspecial.SPECIALScoreboard.inst;
import static com.levviata.lspecial.potion.PotionAmplifiedRegeneration.AMPLIFIED_REGENERATION_NAME;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, guiFactory = "com.levviata.lspecial.LSPECIALGuiFactory")
public class LSPECIALMod {
    // AI IS FREE SLAVERY
    /* NTM:CE source is now available for inspection, including EntityBulletBaseMK4 and RenderBulletMK4. Standard flamer visuals now log renderer cancellation, trail particle creation, projectile motion and GUI texture drawing to diagnose visibility issues. No build files are included in the addon src archive, so a full project build could not be verified. Rocket-jump sound files remain missing. */
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static void info(String message, Object... params) {
        if (LSPECIALConfig.loggingInfo) {
            LOGGER.info(message, params);
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
        info("[RocketDebug] rocket diagnostics initialized: infoLogging={} selfDamageProtection={} rocketJumpPerk={} intelligenceEnabled={} requiredIntelligence={}",
                LSPECIALConfig.loggingInfo, LSPECIALConfig.stingerSelfDamageProtection,
                LSPECIALConfig.rocketJumpLesserPerk, LSPECIALConfig.intelligence,
                LSPECIALConfig.rocketJumpLesserPerkLevel);
        EntityRegistry.registerModEntity(new ResourceLocation(Tags.MOD_ID, "grapple_chain"),
                EntityGrappleChain.class, "grapple_chain", 1, this,  64, 1, true);
        MinecraftForge.EVENT_BUS.register(new GrappleChainSpawnHandler());
        MinecraftForge.EVENT_BUS.register(new BobbleheadUse());
        MinecraftForge.EVENT_BUS.register(new StatsLogic());
        MinecraftForge.EVENT_BUS.register(new SPECIALGameplayHandler());
        MinecraftForge.EVENT_BUS.register(new SPECIALRenamer());
        MinecraftForge.EVENT_BUS.register(new BobbleheadRecipeManager());
        MinecraftForge.EVENT_BUS.register(new LSPECIALContainerOpenHandler());

        AMPLIFIED_REGENERATION.setRegistryName(Tags.MOD_ID, AMPLIFIED_REGENERATION_NAME);

        ForgeRegistries.POTIONS.register(AMPLIFIED_REGENERATION);

        NETWORK.registerMessage(RequestSPECIALStatsPacket.Handler.class, RequestSPECIALStatsPacket.class, 0, Side.SERVER);
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
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (LSPECIALConfig.setSpeedCommand) event.registerServerCommand(new CommandSetSpeed());
        if (LSPECIALConfig.setFlySpeedCommand) event.registerServerCommand(new CommandSetFlySpeed());
    }

    public static void syncStats(EntityPlayerMP player) {
        NETWORK.sendTo(new SyncSPECIALStatsPacket(inst.getStrengthScore().getScorePoints(), inst.getPerceptionScore().getScorePoints(), inst.getEnduranceScore().getScorePoints(),
                inst.getEnduranceBonusScore().getScorePoints(), inst.getCharismaScore().getScorePoints(), inst.getCharismaBonusScore().getScorePoints(),
                inst.getIntelligenceScore().getScorePoints(), inst.getAgilityScore().getScorePoints(), inst.getLuckScore().getScorePoints(),
                inst.getLimitScore().getScorePoints()), player);
    }
/*

    public static String appendModID(String value) {
        return Tags.MOD_ID + ":" + value;
    }*/
}
