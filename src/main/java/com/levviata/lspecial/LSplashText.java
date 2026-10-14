package com.levviata.lspecial;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.util.Random;

public class LSplashText {

    private final Random RANDOM = new Random();

    private final String[] CUSTOM_SPLASHES = {
            "You are SPECIAL!",
            "Vit-o-Matic Vigor Tester",
            "The flesh obeys the will"
    };

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!LSPECIALConfig.showLSplashText || !(event.getGui() instanceof GuiMainMenu)) {
            return;
        }

        // 1 in 20 chance to use one of my custom splashes
        if (RANDOM.nextInt(20) != 0) {
            return;
        }

        try {
            Field field = ReflectionHelper.findField(GuiMainMenu.class, "splashText", "field_73975_c");
            field.setAccessible(true);

            String splash = CUSTOM_SPLASHES[RANDOM.nextInt(CUSTOM_SPLASHES.length)];
            field.set(event.getGui(), splash);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // a missing splash field should not produce a stack trace every time the menu opens.
        }
    }
}