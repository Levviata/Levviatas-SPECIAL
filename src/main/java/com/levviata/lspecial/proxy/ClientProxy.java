package com.levviata.lspecial.proxy;

import com.levviata.lspecial.*;
import com.levviata.lspecial.client.ClientGunDebugHandler;
import com.levviata.lspecial.client.render.RenderGrappleChain;
import com.levviata.lspecial.entity.EntityGrappleChain;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy { // client only
    public static final KeyBinding toggleSpecial = new KeyBinding("toggleSpecial", Keyboard.KEY_P, "key.categories.misc");

    public static KeyBinding toggleSpecial = new KeyBinding(
            "toggleSpecial",
            Keyboard.KEY_P,
            "key.categories.misc"
    );

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityGrappleChain.class, RenderGrappleChain::new);
        MinecraftForge.EVENT_BUS.register(new GuiSPECIAL());
        MinecraftForge.EVENT_BUS.register(new LSplashText());
        MinecraftForge.EVENT_BUS.register(new PlayerModelScaleHandler());
        MinecraftForge.EVENT_BUS.register(new GrappleOverlayHandler());
        MinecraftForge.EVENT_BUS.register(new TF2AmmoOverlay());
        MinecraftForge.EVENT_BUS.register(new GrappleClientHandler());
        MinecraftForge.EVENT_BUS.register(new TooltipAdder());
        MinecraftForge.EVENT_BUS.register(new ClientGunDebugHandler());
        MinecraftForge.EVENT_BUS.register(new SPECIALRenamer());
        if (LSPECIALConfig.toggleSpecial) {
            ClientRegistry.registerKeyBinding(toggleSpecial);
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
