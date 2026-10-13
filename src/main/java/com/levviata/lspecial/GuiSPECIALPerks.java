package com.levviata.lspecial;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** small, scrollable overlay for the non-weapon SPECIAL perk descriptions. */
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
        int w = Math.max(1, Math.min(360, width - 16));
        int h = Math.max(1, Math.min(250, height - 16));
        int x = Math.max(0, (width - w) / 2), y = Math.max(0, (height - h) / 2);
        drawRect(0, 0, width, height, 0x88000000);
        drawRect(x, y, x + w, y + h, PANEL);
        drawRect(x, y, x + w, y + 1, MUTED); drawRect(x, y + h - 1, x + w, y + h, MUTED);
        drawRect(x, y, x + 1, y + h, MUTED); drawRect(x + w - 1, y, x + w, y + h, MUTED);

        int innerX = x + Math.min(12, Math.max(2, w / 12));
        int innerWidth = Math.max(1, w - (innerX - x) * 2);
        String title = fontRenderer.trimStringToWidth(stat.toUpperCase() + " // pERKS", innerWidth);
        drawString(fontRenderer, title, innerX, y + Math.min(12, Math.max(1, h / 10)), BRIGHT);
        if (h > 30) drawRect(x + 8, y + 28, x + w - 8, y + 29, MUTED);

        int contentTop = y + 40;
        int footerY = y + h - 12;
        int lineHeight = 12;
        int visible = Math.max(0, (footerY - contentTop - 2) / lineHeight);
        List<String> wrapped = getWrappedLines(innerWidth);
        int maxScroll = Math.max(0, wrapped.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < visible && scroll + i < wrapped.size(); i++) {
            String line = wrapped.get(scroll + i);
            int color = line.startsWith("LESSER") || line.startsWith("HIGHER") ? BRIGHT : AMBER;
            drawString(fontRenderer, fontRenderer.trimStringToWidth(line, innerWidth), innerX,
                    contentTop + i * lineHeight, color);
        }
        String hint = "Wheel / arrows to scroll - ESC to return";
        drawString(fontRenderer, fontRenderer.trimStringToWidth(hint, innerWidth), innerX,
                Math.max(y + 1, footerY), MUTED);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private List<String> getWrappedLines(int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String text : lines) {
            String remaining = text;
            if (remaining.isEmpty()) { result.add(""); continue; }
            while (!remaining.isEmpty()) {
                String part = fontRenderer.trimStringToWidth(remaining, Math.max(1, maxWidth));
                if (part.isEmpty()) break;
                if (part.length() < remaining.length()) {
                    int split = part.lastIndexOf(' ');
                    if (split > 0) part = part.substring(0, split);
                }
                result.add(part);
                remaining = remaining.substring(part.length()).trim();
            }
        }
        return result;
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

    private int clamp(int value) {
        int w = Math.max(1, Math.min(360, width - 16));
        int innerWidth = Math.max(1, w - 24);
        int h = Math.max(1, Math.min(250, height - 16));
        int visible = Math.max(0, (h - 24 - 40 - 2) / 12);
        return Math.max(0, Math.min(value, Math.max(0, getWrappedLines(innerWidth).size() - visible)));
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
