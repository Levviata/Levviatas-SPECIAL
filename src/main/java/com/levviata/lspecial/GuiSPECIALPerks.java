package com.levviata.lspecial;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Small, scrollable overlay for the non-weapon SPECIAL perk descriptions. */
public final class GuiSPECIALPerks extends GuiScreen {
    private static final int AMBER = 0xFFFFC45A;
    private static final int BRIGHT = 0xFFFFD477;
    private static final int MUTED = 0xFFB98236;
    private static final int PANEL = 0xE9000805;
    private final GuiScreen parent;
    private final String stat;
    private final List<String> lines = new ArrayList<>();
    private int scroll;

    public GuiSPECIALPerks(GuiScreen parent, String stat) {
        this.parent = parent;
        this.stat = stat;
        populate();
    }

    private void populate() {
        lines.clear();
        switch (stat) {
            case "Strength":
                lines.add("LESSER PERK — Level 5");
                lines.add("Attack damage bonus follows the configured Strength formula.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Further bonuses depend on the active configuration.");
                break;
            case "Perception":
                lines.add("LESSER PERK — Level 5");
                lines.add("Found-loot duplication is planned for supported loot inventories.");
                lines.add("HIGHER PERK — Level 10");
                lines.add("Higher-tier loot effects require a compatible loot-generation hook.");
                lines.add("HIGHER PERK — Above level 15");
                lines.add("Additional armor interaction depends on the NTM API.");
                break;
            case "Endurance":
                lines.add("LESSER PERK — Level 5");
                lines.add("Enables configured health-related effects.");
                lines.add("The optional model scale affects rendering only, not collision or reach.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Further effects depend on enabled Endurance settings.");
                break;
            case "Charisma":
                lines.add("LESSER PERK — Level 5");
                lines.add("Configured entities may ignore the player when supported.");
                lines.add("Villager trade discounts can be enabled separately.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Additional effects depend on the active configuration.");
                break;
            case "Agility":
                lines.add("LESSER PERK — Level 5");
                lines.add("Movement speed follows the configured Agility formula.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Flying speed remains separate from ground movement speed.");
                break;
            case "Luck":
                lines.add("LESSER PERK — Level 5");
                lines.add("Durability protection uses a configurable chance and cap.");
                lines.add("HIGHER PERK — Level 10");
                lines.add("Additional loot effects depend on a compatible loot-generation hook.");
                break;
            case "Intelligence":
                lines.add("LESSER PERK — Level 5");
                lines.add("See the Guns and Ammunition configuration for currently enabled effects.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("See the Guns and Ammunition configuration for currently enabled effects.");
                break;
            default:
                lines.add("No perk notes are available for this stat.");
        }
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int w = Math.min(360, width - 24), h = Math.min(250, height - 24);
        int x = (width - w) / 2, y = (height - h) / 2;
        drawRect(0, 0, width, height, 0x88000000);
        drawRect(x, y, x + w, y + h, PANEL);
        drawRect(x, y, x + w, y + 1, MUTED); drawRect(x, y + h - 1, x + w, y + h, MUTED);
        drawRect(x, y, x + 1, y + h, MUTED); drawRect(x + w - 1, y, x + w, y + h, MUTED);
        drawString(fontRenderer, stat.toUpperCase() + " // PERKS", x + 12, y + 12, BRIGHT);
        drawRect(x + 10, y + 28, x + w - 10, y + 29, MUTED);
        int yy = y + 40;
        int visible = Math.max(1, (h - 65) / 20);
        for (int i = scroll; i < Math.min(lines.size(), scroll + visible); i++) {
            String line = lines.get(i);
            int color = line.startsWith("LESSER") || line.startsWith("HIGHER") ? BRIGHT : AMBER;
            drawString(fontRenderer, line, x + 12, yy, color);
            yy += 20;
        }
        drawString(fontRenderer, "Mouse wheel / arrows to scroll  •  ESC to return", x + 12, y + h - 16, MUTED);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) scroll = clamp(scroll + (wheel < 0 ? 1 : -1));
    }

    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) { mc.displayGuiScreen(parent); return; }
        if (keyCode == Keyboard.KEY_DOWN) scroll = clamp(scroll + 1);
        if (keyCode == Keyboard.KEY_UP) scroll = clamp(scroll - 1);
        super.keyTyped(typedChar, keyCode);
    }

    private int clamp(int value) { return Math.max(0, Math.min(value, Math.max(0, lines.size() - 1))); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
