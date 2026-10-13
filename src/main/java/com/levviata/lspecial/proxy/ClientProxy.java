package com.levviata.lspecial.proxy;

import com.levviata.lspecial.*;
import net.minecraftforge.common.MinecraftForge;
import com.levviata.lspecial.network.SyncSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsSetter;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import static com.levviata.lspecial.LSPECIALMod.toggleSpecial;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy { // client only

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new PlayerModelScaleHandler());
        MinecraftForge.EVENT_BUS.register(new GrappleOverlayHandler());
        MinecraftForge.EVENT_BUS.register(new StrengthTooltipHandler());
        if (com.levviata.lspecial.LSPECIALConfig.toggleSpecial) ClientRegistry.registerKeyBinding(toggleSpecial);
        LSPECIALMod.NETWORK.registerMessage(SyncSPECIALStatsSetter.class, SyncSPECIALStatsPacket.class, 1, Side.CLIENT);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
