package com.levviata.lspecial;

import com.levviata.lspecial.command.CommandSetFlySpeed;
import com.levviata.lspecial.command.CommandSetSpeed;
import com.levviata.lspecial.network.RequestSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsSetter;
import com.levviata.lspecial.potion.PotionAmplifiedRegeneration;
import com.levviata.lspecial.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextComponentString;
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
    /* NTM:CE source is available and RenderBulletMK4 delegates to BulletConfig.renderer inside a balanced push/pop matrix. Standard flamer configs had no renderer, so a client-only cached BulletConfig clone now supplies a dedicated renderer without changing the synced config ID or shared NTM config. Airblast particles now use WorldServer's synchronized particle path and a plane perpendicular to aim. Flamethrower TF2-rework pass -> the no-gravity/distance-based speed/size/color system was already wired through MixinTF2FlameProjectile + MixinTF2FlameRender + TF2FlameRenderer/TF2FlameParticle, but deleted an orphaned, unregistered duplicate (MixinTF2FlameParticleTrail, never in mixins.lspecial.json) that would've double-spawned particles with mismatched colors/random seed if someone ever enabled it. Native flame velocity (1 block/tick) crossed the whole 8-block range in ~1s, way before the 5s size-growth curve finished, so flames always died tiny; TF2FlamethrowerHandler#initFlameTuning (postInit, after XFactoryFlamer.init()) now slows flame_diesel/gas/napalm/balefire/nograv to 0.2F so both curves land around the same ~100 ticks, and raises every standard flame config's expires to a shared floor of 140 ticks since flame_gas's native expires=10 would've killed it almost instantly otherwise - the 8-block distance check is what actually ends the flame's life now, not NTM's native timer. Also made the orphaned-shooter case (shooter died/disconnected mid-burst) explicitly setDead() the flame instead of coasting on its last motion. Color bands tightened to exactly 0-2 / 2-5 / 5-8 blocks per spec. Two more orphaned/unregistered duplicate mixins from an earlier pass are still sitting in mixin/ (MixinDragonFuryConfig.java, MixinDragonFuryProjectile.java - singular "Dragon", the registered ones are the plural "Dragons" versions) - left alone since they're Dragon's Fury (topaz) specific and out of scope here, but worth deleting. Full compilation still not verified, no build files in the addon archive and no network access available. Rocket-jump reward sounds -> rocket_refund3.ogg was supplied and wired; rocket_refund1.ogg and rocket_refund2.ogg were named but not attached, so their registered resources are still missing. Rocket-jump rewards are one-shot: a successful jump arms the reward and only the next successful jump can redeem it. Standard flamethrower and Dragon's Fury perk logic now have separate false-by-default config toggles. Luck armor pass -> armor protection now follows vanilla Unbreaking-style per-durability-point odds with Luck as the effective level; powered armor energy protection uses the same per-point logic. Existing armor/power-armor failsafes were left in place. Full compilation is still not verified because the addon archive has no build files. */
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
        info("[RocketDebug] rocket diagnostics initialized: infoLogging={} selfDamageProtection={} rocketJumpPerk={} intelligenceEnabled={} requiredIntelligence={}",
                LSPECIALConfig.loggingInfo, LSPECIALConfig.stingerSelfDamageProtection,
                LSPECIALConfig.rocketJumpLesserPerk, LSPECIALConfig.intelligence,
                LSPECIALConfig.rocketJumpLesserPerkLevel);
        EntityRegistry.registerModEntity(new ResourceLocation(Tags.MOD_ID, "grapple_chain"),
                EntityGrappleChain.class, "grapple_chain", 1, this, 64, 1, true);
        MinecraftForge.EVENT_BUS.register(new GrappleChainSpawnHandler());
        MinecraftForge.EVENT_BUS.register(new BobbleheadUse());
        MinecraftForge.EVENT_BUS.register(new StatsLogic());
        MinecraftForge.EVENT_BUS.register(new SPECIALGameplayHandler());
        MinecraftForge.EVENT_BUS.register(new SPECIALRenamer());
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
