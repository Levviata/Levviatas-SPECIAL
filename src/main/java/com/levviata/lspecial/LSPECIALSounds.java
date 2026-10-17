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
    public static SoundEvent DRAGONS_FURY_SHOOT_CRIT;
    public static SoundEvent DRAGONS_FURY_IMPACT;
    public static SoundEvent DRAGONS_FURY_IMPACT_MINICRIT;
    public static SoundEvent DRAGONS_FURY_IMPACT_CRIT;
    public static SoundEvent GRAPPLE_START;
    public static SoundEvent GRAPPLE_CYCLE;
    public static SoundEvent GRAPPLE_OUT;
    public static SoundEvent GRAPPLE_RESET;
    public static SoundEvent GRAPPLE_RESETCLINK;

    private LSPECIALSounds() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        DRAGONS_FURY_SHOOT = register(event, "dragons_fury_shoot");
        DRAGONS_FURY_SHOOT_CRIT = register(event, "dragons_fury_shoot_crit");
        DRAGONS_FURY_IMPACT = register(event, "dragons_fury_impact");
        DRAGONS_FURY_IMPACT_MINICRIT = register(event, "dragons_fury_impact_bonus_damage");
        DRAGONS_FURY_IMPACT_CRIT = register(event, "dragons_fury_impact_bonus_damage_pain");
        GRAPPLE_START = register(event, "grapple_start");
        GRAPPLE_CYCLE = register(event, "grapple_cycle");
        GRAPPLE_OUT = register(event, "grapple_out");
        GRAPPLE_RESET = register(event, "grapple_reset");
        GRAPPLE_RESETCLINK = register(event, "grapple_resetclink");
    }

    private static SoundEvent register(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
