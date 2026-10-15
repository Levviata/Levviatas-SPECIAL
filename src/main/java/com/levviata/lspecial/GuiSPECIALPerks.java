package com.levviata.lspecial;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

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
            /*case "Strength":
                lines.add("LESSER PERK — Level 5");
                lines.add("Attack damage bonus follows the configured Strength formula.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Further bonuses depend on the active configuration.");
                break;*/
            case "Perception":
                lines.add("LESSER PERK — Level 5");
                lines.add("Duplicates found loot");
                lines.add("HIGHER PERK — Level 10");
                lines.add("If Luck is at level 10, add new special items to found loot");
                lines.add("HIGHER PERK — Above level 15");
                lines.add("+1% armor threshold reduction per level, applies to guns");
                break;
            case "Endurance":
                lines.add("LESSER PERK — Level 5");
                lines.add("Enlarges your player model (not including hitbox and reach) proportional to your current level");
               /* lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Further effects depend on enabled Endurance settings.");*/
                break;
            case "Charisma":
                lines.add("LESSER PERK — Level 5");
                lines.add("Configured entities may ignore the player when supported.");
                lines.add("Villagers have a discount in proportion to your current level. Your current discount is " + getVillagerDiscountPercent() + "%");
                /*lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Additional effects depend on the active configuration.");*/
                break;
            /*case "Agility":
                lines.add("LESSER PERK — Level 5");
                lines.add("Movement speed follows the configured Agility formula.");
                lines.add("HIGHER PERK — Levels 10–15");
                lines.add("Flying speed remains separate from ground movement speed.");
                break;*/
            case "Luck":
                lines.add("LESSER PERK — Level 5");
                lines.add("Chance proportional to your current level to avoid losing durability for current armor equipped");
                lines.add("HIGHER PERK — Level 10");
                lines.add("If perception level 10, add new special items to found loot.");
                break;
            case "Intelligence":
                lines.add("LESSER PERK — Level 5");
                lines.add("Alternative firing modes for many guns. Toggles with Secondary Fire (keybind):");
                lines.add("Comically Long gun, 3 bullet burst. Shift-Secondary Fire to kill yourself, can be disabled");
                lines.add("Star-F, UZI, G3: unlock alternate ammunition modes where configured.");
                lines.add("HIGHER PERK — Levels 15");
                lines.add("All Revolvers: +1 penetration and x3 headshot multiplier");
                lines.add("Henry Lincoln Repeater: 3 bullet burst");
                lines.add("Star-F: BMG ammunition uses a 10-round magazine.");
                lines.add("AMAT: crouch + right-click selects a 1-round deuterium capsule magazine; 2x Laser damage, 50% protection negation, homes to Quackos.");
                lines.add("Carbine: increases magazine capacity, enables semi-automatic secondary fire, and reduces recoil.");
                lines.add("Minigun: accepts Capacitor ammo.");
                lines.add("Double Barrel: entity grappling on crouch + Second Fire.");
                lines.add("Topaz (Dragon's Fury): secondary fire gains increased direct-hit damage and short-range projectiles.");
                lines.add("Flamer (Phlogistinator): secondary projectiles gain a wider hitbox and short range.");
                lines.add("Tau Cannon: charged secondary fire deals increased damage.");
                lines.add("Fat Man: enables its configured higher-perk firing-delay adjustment.");
                lines.add("Laser Pistol: applies its configured higher-perk firing-delay adjustment.");
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
        String title = fontRenderer.trimStringToWidth(stat.toUpperCase() + " // PERKS", innerWidth);
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

    private static int getVillagerDiscountPercent() {
        if (!LSPECIALConfig.charisma || !LSPECIALConfig.charismaVillagerDiscount) {
            return 0;
        }

        int charisma = inst.getCharismaScore().getScorePoints();

        if (charisma < LSPECIALConfig.charismaVillagerDiscountLevel) {
            return 0;
        }

        Map<String, Double> variables = new HashMap<>();
        variables.put("charisma", (double) charisma);

        double fraction = LSPECIALExpression.eval(
                LSPECIALConfig.charismaVillagerDiscountFormula,
                variables,
                0.0D
        );

        fraction = Math.max(0.0D, Math.min(0.75D, fraction));

        return (int) Math.round(fraction * 100.0D);
    }
}
