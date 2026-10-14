package com.levviata.lspecial.proxy;

import com.levviata.lspecial.*;
import com.levviata.lspecial.network.SyncSPECIALStatsPacket;
import com.levviata.lspecial.network.SyncSPECIALStatsSetter;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy { // client only
    public static final KeyBinding toggleSpecial = new KeyBinding("toggleSpecial", Keyboard.KEY_P, "key.categories.misc");

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new GuiSPECIAL());
        MinecraftForge.EVENT_BUS.register(new LSplashText());
        MinecraftForge.EVENT_BUS.register(new PlayerModelScaleHandler());
        MinecraftForge.EVENT_BUS.register(new GrappleOverlayHandler());
        MinecraftForge.EVENT_BUS.register(new TooltipAdder());
        if (LSPECIALConfig.toggleSpecial) {
            ClientRegistry.registerKeyBinding(toggleSpecial);
        }
        LSPECIALMod.NETWORK.registerMessage(SyncSPECIALStatsSetter.class, SyncSPECIALStatsPacket.class, 1, Side.CLIENT);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
