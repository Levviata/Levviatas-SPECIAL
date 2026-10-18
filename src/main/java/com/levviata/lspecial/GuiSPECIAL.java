package com.levviata.lspecial;

import com.github.bsideup.jabel.Desugar;
import com.levviata.lspecial.network.RequestSPECIALStatsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.ArrayList;
import java.util.List;

import static com.levviata.lspecial.proxy.ClientProxy.toggleSpecial;
import static com.levviata.lspecial.SPECIALScoreboard.inst;

public class GuiSPECIAL extends GuiScreen {

    private static final int AMBER = 0xFFFFC45A;
    private static final int AMBER_BRIGHT = 0xFFFFD477;
    private static final int AMBER_MUTED = 0xFFB98236;
    private static final int PANEL = 0xE9000805;
    private static final int PANEL_DARK = 0xD9000503;
    private static final int ROW_HEIGHT = 22;

    private final List<StatRow> rows = new ArrayList<>();

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int selectedRow = -1;

    private String footerMessage;

    @Override
    public void initGui() {
        super.initGui();
        rebuildRows();
        footerMessage = chooseFooterMessage();
    }

    private String chooseFooterMessage() {
        String configuredMessages = LSPECIALConfig.randomFooterMessages;
        if (configuredMessages == null || configuredMessages.trim().isEmpty()) {
            return "";
        }

        String[] messages = configuredMessages.split("\\|");
        List<String> available = new ArrayList<>();
        for (String message : messages) {
            String trimmed = message.trim();
            if (!trimmed.isEmpty()) {
                available.add(trimmed);
            }
        }

        if (available.isEmpty()) {
            return "";
        }
        return available.get(new java.util.Random().nextInt(available.size()));
    }

    // clicking a stat opens its lesser/higher perk overlay when enabled.
    private void rebuildRows() {
        rows.clear();

        addStat("Strength", inst.getClientStrength(), LSPECIALConfig.strength, "Adds attack damage. The held-item tooltip displays the total, including the Strength bonus.");
        addStat("Perception", inst.getClientPerception(), LSPECIALConfig.perception, "Increase gun damage.");
        addStat("Endurance", inst.getClientEndurance(), LSPECIALConfig.endurance, "Increases maximum health");

        if (LSPECIALConfig.enduranceBonus) {
            rows.add(new StatRow("Endurance Bonus", inst.getClientEnduranceBonus(), true, "Endurance Level 5. Adds amplified regeneration, which amplifies current regeneration by +1 or gives Regeneration I."));
        }

        addStat("Charisma", inst.getClientCharisma(), LSPECIALConfig.charisma, "Increases armor and armor toughness; higher-level social effects are configurable.");

        if (LSPECIALConfig.charismaBonus) {
            rows.add(new StatRow("Charisma Bonus", inst.getClientCharismaBonus(), true, "Charisma Level 5. Grants permanent Resistance."));
        }

        String m1 = "";
        if (LSPECIALConfig.gunWearMode.equals("PERCENT_REDUCTION")) {
            m1 = "reduces gun wear by a percentage";
        }
        if (LSPECIALConfig.gunWearMode.equals("INFINITE_AT_INTELLIGENCE_5")) {
            m1 = "infinite gun durability";
        }
        if (LSPECIALConfig.gunWearMode.equals("FORMULA")) {
            m1 = "reduces gun wear";
        }
        String m2 = "The flesh obeys the will. Increases gun fire rate, " + m1 + ", and infinite item durability.";
        addStat("Intelligence", inst.getClientIntelligence(), LSPECIALConfig.intelligence, m2);

        addStat("Agility", inst.getClientAgility(), LSPECIALConfig.agility, "Increases movement speed including flight speed. Can be configured with /setspeed or /setflyspeed.");
        addStat("Luck", inst.getClientLuck(), LSPECIALConfig.luck, "Chance to refund ammunition when reloading a gun.");

        if (LSPECIALConfig.statLimit) {
            rows.add(new StatRow("Stat Limit", inst.getClientLimit(), true, "Maximum level for each SPECIAL stat."));
        }
    }

    private void addStat(String name, int value, boolean enabled, String description) {
        if (enabled || !LSPECIALConfig.hideDisabledStats) {
            rows.add(new StatRow(name, value, enabled, description));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        rebuildRows();

        // keep the panel and all of its content within even unusually small GUI resolutions.
        panelWidth = Math.max(1, Math.min(620, width - 24));
        panelHeight = Math.max(1, Math.min(Math.max(1, height - 16), Math.max(250, 104 + rows.size() * ROW_HEIGHT)));
        panelLeft = Math.max(0, (width - panelWidth) / 2);
        panelTop = Math.max(0, (height - panelHeight) / 2);

        drawBackdrop();
        drawFrame();
        drawHeader();
        drawStatList(mouseX, mouseY);
        drawInformationPanel();
        drawFooter();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackdrop() {
        drawRect(0, 0, width, height, 0x55000000);

        for (int y = 0; y < height; y += 4) {
            drawRect(0, y, width, y + 1, 0x10000000);
        }

        drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL);
        drawRect(panelLeft + 3, panelTop + 3, panelLeft + panelWidth - 3, panelTop + panelHeight - 3, PANEL_DARK);
    }

    private void drawFrame() {
        int right = panelLeft + panelWidth;
        int bottom = panelTop + panelHeight;

        drawRect(panelLeft, panelTop, right, panelTop + 1, AMBER_MUTED);
        drawRect(panelLeft, bottom - 1, right, bottom, AMBER_MUTED);
        drawRect(panelLeft, panelTop, panelLeft + 1, bottom, AMBER_MUTED);
        drawRect(right - 1, panelTop, right, bottom, AMBER_MUTED);

        drawRect(panelLeft + 22, panelTop, panelLeft + 238, panelTop + 2, PANEL);
    }

    private void drawHeader() {
        drawClippedString("S.P.E.C.I.A.L // CHARACTER STATS", panelLeft + 22, panelTop + 12,
                Math.max(1, panelWidth - 44), AMBER_BRIGHT);

        drawRect(panelLeft + 18, panelTop + 30, panelLeft + panelWidth - 18, panelTop + 31, AMBER_MUTED);
    }

    private void drawStatList(int mouseX, int mouseY) {
        int listLeft = panelLeft + 18;
        int listTop = panelTop + 42;
        int listWidth = Math.min(270, panelWidth / 2 - 22);

        drawClippedString("STATS", listLeft + 8, listTop, Math.max(1, listWidth - 62), AMBER_MUTED);
        drawClippedString("VALUE", listLeft + listWidth - 48, listTop, 44, AMBER_MUTED);

        int rowTop = listTop + 17;
        selectedRow = -1;

        for (int i = 0; i < rows.size(); i++) {
            StatRow row = rows.get(i);
            int y = rowTop + i * ROW_HEIGHT;

            if (y + ROW_HEIGHT > panelTop + panelHeight - 35) {
                break;
            }

            boolean hovered = mouseX >= listLeft && mouseX < listLeft + listWidth
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

            if (hovered) {
                selectedRow = i;
                drawRect(listLeft, y - 2, listLeft + listWidth, y + ROW_HEIGHT - 3,
                        0x553F2A0C);
                drawRect(listLeft, y - 2, listLeft + 2, y + ROW_HEIGHT - 3, AMBER);
            }

            int color = hovered ? AMBER_BRIGHT : (row.enabled ? AMBER : AMBER_MUTED);
            String label = row.enabled ? row.name : "\u00a7m" + row.name + "\u00a7r";
            int labelX = listLeft + 9;
            int valueX = listLeft + listWidth - 42;
            drawClippedString(label, labelX, y + 4, Math.max(1, valueX - labelX - 5), color);

            String value = row.name.endsWith("Bonus")
                    ? Boolean.toString(row.value == 1) : Integer.toString(row.value);
            drawClippedString(value, valueX, y + 4, Math.max(1, listLeft + listWidth - 4 - valueX), color);
        }

        int dividerX = panelLeft + panelWidth / 2 - 2;
        drawRect(dividerX, panelTop + 42, dividerX + 1,
                panelTop + panelHeight - 28, 0xFF62451F);
    }

    private void drawInformationPanel() {
        int infoLeft = panelLeft + panelWidth / 2 + 12;
        int infoTop = panelTop + 48;
        int infoRight = panelLeft + panelWidth - 18;
        int infoWidth = infoRight - infoLeft;

        drawClippedString("NOTES", infoLeft + 8, infoTop, Math.max(1, infoWidth - 16), AMBER_MUTED);
        drawRect(infoLeft, infoTop + 17, infoRight, infoTop + 18, AMBER_MUTED);

        String heading = selectedRow >= 0 && selectedRow < rows.size()
                ? rows.get(selectedRow).name.toUpperCase()
                : "S.P.E.C.I.A.L";
        drawClippedString(heading, infoLeft + 8, infoTop + 29, Math.max(1, infoWidth - 16), AMBER_BRIGHT);

        String description = selectedRow >= 0 && selectedRow < rows.size()
                ? rows.get(selectedRow).description
                : "Select a stat to see what it does.";

        int textX = infoLeft + 8;
        int textWidth = Math.max(1, infoWidth - 16);
        int statusY = panelTop + panelHeight - 67;
        int textBottom = LSPECIALConfig.showSystemStatusInfo ? statusY - 5 : panelTop + panelHeight - 30;
        int textY = infoTop + 49;
        int availableLines = Math.max(0, (textBottom - textY) / 12);
        int usedLines = drawWrappedText(description, textX, textY, textWidth, AMBER, availableLines);
        textY += usedLines * 12;
        if (LSPECIALConfig.showPerkTierHint && textY < textBottom) {
            textY += 3;
            drawWrappedText("Lesser perks are usually obtained at level 5; Higher perks at levels 10–15. Click a stat row to inspect its perk notes.",
                    textX, textY, textWidth, AMBER_MUTED, Math.max(0, (textBottom - textY) / 12));
        }

        if (LSPECIALConfig.showSystemStatusInfo) {
            drawRect(infoLeft, statusY, infoRight, statusY + 1, AMBER_MUTED);
            int statusTextWidth = Math.max(1, infoRight - (infoLeft + 8) - 6);
            drawClippedString(LSPECIALConfig.systemStatusText,
                    infoLeft + 8, statusY + 9, statusTextWidth, AMBER_MUTED);
            drawClippedString(LSPECIALConfig.neuralLinkText,
                    infoLeft + 8, statusY + 24, statusTextWidth, AMBER);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || !LSPECIALConfig.showPerkScreens) return;
        int listLeft = panelLeft + 18;
        int listTop = panelTop + 42;
        int listWidth = Math.min(270, panelWidth / 2 - 22);
        int rowTop = listTop + 17;
        int maxBottom = panelTop + panelHeight - 35;
        for (int i = 0; i < rows.size(); i++) {
            int y = rowTop + i * ROW_HEIGHT;
            if (y + ROW_HEIGHT > maxBottom) break;
            if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                StatRow row = rows.get(i);
                if (row.enabled && !"Stat Limit".equals(row.name) && !row.name.endsWith("Bonus")) {
                    mc.displayGuiScreen(new GuiSPECIALPerks(this, row.name));
                }
                return;
            }
        }
    }

    private int drawWrappedText(String text, int x, int y, int maxWidth, int color, int maxLines) {
        if (text == null || text.isEmpty() || maxLines <= 0 || maxWidth <= 0) return 0;
        List<String> wrapped = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            // split unusually long words too, so even a narrow GUI cannot overflow horizontally.
            while (fontRenderer.getStringWidth(word) > maxWidth) {
                if (line.length() > 0) {
                    wrapped.add(line.toString());
                    line.setLength(0);
                }
                String part = fontRenderer.trimStringToWidth(word, maxWidth);
                if (part.isEmpty()) break;
                wrapped.add(part);
                word = word.substring(part.length());
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fontRenderer.getStringWidth(candidate) > maxWidth && line.length() > 0) {
                wrapped.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else if (!word.isEmpty()) {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) wrapped.add(line.toString());

        int count = Math.min(maxLines, wrapped.size());
        for (int i = 0; i < count; i++) {
            String visible = wrapped.get(i);
            if (i == maxLines - 1 && wrapped.size() > maxLines) {
                String ellipsis = "...";
                visible = fontRenderer.trimStringToWidth(visible, Math.max(1, maxWidth - fontRenderer.getStringWidth(ellipsis))) + ellipsis;
            }
            drawString(fontRenderer, visible, x, y + i * 12, color);
        }
        return count;
    }

    /**
     * Draws a single line within its allotted width, adding an ellipsis when clipped.
     */
    private void drawClippedString(String text, int x, int y, int maxWidth, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return;
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            drawString(fontRenderer, text, x, y, color);
            return;
        }
        String ellipsis = "...";
        int textWidth = Math.max(1, maxWidth - fontRenderer.getStringWidth(ellipsis));
        String clipped = fontRenderer.trimStringToWidth(text, textWidth);
        drawString(fontRenderer, clipped + ellipsis, x, y, color);
    }

    private void drawFooter() {
        int footerY = panelTop + panelHeight - 23;
        String hint = "PRESS ESC TO CLOSE";
        int hintWidth = Math.min(fontRenderer.getStringWidth(hint), Math.max(1, panelWidth - 16));
        int hintX = Math.max(panelLeft + 4, panelLeft + panelWidth - hintWidth - 10);

        if (LSPECIALConfig.showRandomFooter && footerMessage != null && !footerMessage.isEmpty()) {
            drawRect(panelLeft + Math.min(18, panelWidth / 2), footerY - 5,
                    Math.max(panelLeft + 1, panelLeft + panelWidth - Math.min(18, panelWidth / 2)), footerY - 4, AMBER_MUTED);
            int messageX = panelLeft + Math.min(22, Math.max(2, panelWidth / 4));
            int messageWidth = Math.max(1, hintX - messageX - 8);
            drawClippedString("LSPECIAL  /  " + footerMessage, messageX, footerY + 2, messageWidth, AMBER_MUTED);
        }

        drawClippedString(hint, hintX, footerY + 2, Math.max(1, panelLeft + panelWidth - hintX - 4), AMBER);
    }

    @Desugar
    private record StatRow(String name, int value, boolean enabled, String description) {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!LSPECIALConfig.toggleSpecial || !toggleSpecial.isPressed()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft.currentScreen instanceof GuiSPECIAL) {
            minecraft.displayGuiScreen(null);
        } else {
            LSPECIALMod.NETWORK.sendToServer(new RequestSPECIALStatsPacket());
            minecraft.displayGuiScreen(new GuiSPECIAL());
        }
    }
}