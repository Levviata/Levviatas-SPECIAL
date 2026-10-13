package com.levviata.lspecial.potion;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;

public class PotionAmplifiedRegeneration extends Potion {
    public static final String AMPLIFIED_REGENERATION_NAME = "amplified_regeneration";

    public PotionAmplifiedRegeneration() {
        super(false, 0xCD5CAB); // same color as vanilla Regeneration

        setPotionName("effect." + AMPLIFIED_REGENERATION_NAME);
        setIconIndex(7, 0); // regeneration icon
    }

    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0F);
        }
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        int interval = 50 >> amplifier;
        return interval > 0 ? duration % interval == 0 : true;
    }
}
