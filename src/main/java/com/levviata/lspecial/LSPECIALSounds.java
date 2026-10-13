package com.levviata.lspecial;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** registers the custom weapon sounds used by the secondary-fire perks. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class LSPECIALSounds {
    public static SoundEvent DRAGONS_FURY_SHOOT;
    public static SoundEvent PHLOG_IGNITE;

    private LSPECIALSounds() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        DRAGONS_FURY_SHOOT = register(event, "dragons_fury_shoot");
        PHLOG_IGNITE = register(event, "phlog_ignite");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
