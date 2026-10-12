package com.levviata.lspecial;

import com.levviata.lspecial.network.RequestSPECIALStatsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.ArrayList;
import java.util.List;

import static com.levviata.lspecial.LSPECIALMod.toggleSpecial;
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

    private void rebuildRows() {
        rows.clear();

        addStat("Strength", inst.getClientStrength(), LSPECIALConfig.strength, "Gives on-hit attack damage, currently doesn't display on the held item.");
        addStat("Perception", inst.getClientPerception(), LSPECIALConfig.perception, "Increases gun damage.");
        addStat("Endurance", inst.getClientEndurance(), LSPECIALConfig.endurance, "Gives max health, one heart per level.");

        if (LSPECIALConfig.enduranceBonus) {
            rows.add(new StatRow("Endurance Bonus", inst.getClientEnduranceBonus(), true, "Gives you permanent regeneration 2. If you already have regeneration, increase by one it's amplifier (ex: regen 1 to 2)"));
        }

        addStat("Charisma", inst.getClientCharisma(), LSPECIALConfig.charisma, "Increases armor and armor toughness.");

        if (LSPECIALConfig.charismaBonus) {
            rows.add(new StatRow("Charisma Bonus", inst.getClientCharismaBonus(), true, "Gives you permanent resistance effect."));
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
        String m2 = "The flesh obeys the will. Increases gun fire rate, " + m1 + " and gives infinite item durability. At level 5, makes the Pepperbox shot 6 rounds at once. At level 10, makes all revolvers penetrate more than one entity and x3 headshot damage";
        addStat("Intelligence", inst.getClientIntelligence(), LSPECIALConfig.intelligence, m2);

        addStat("Agility", inst.getClientAgility(), LSPECIALConfig.agility, "Gives speed, can be configured with command /setspeed");
        addStat("Luck", inst.getClientLuck(), LSPECIALConfig.luck, "Gives a chance to refund a magazine on gun reload.");

        if (LSPECIALConfig.statLimit) {
            rows.add(new StatRow("Stat Limit", inst.getClientLimit(), true, "The current maximum for each SPECIAL stat."));
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

        panelWidth = Math.min(620, width - 24);
        panelHeight = Math.min(height - 28, Math.max(250, 104 + rows.size() * ROW_HEIGHT));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

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
        drawString(fontRenderer, "S.P.E.C.I.A.L // CHARACTER STATS", panelLeft + 22, panelTop + 12, AMBER_BRIGHT);

        drawRect(panelLeft + 18, panelTop + 30, panelLeft + panelWidth - 18, panelTop + 31, AMBER_MUTED);
    }

    private void drawStatList(int mouseX, int mouseY) {
        int listLeft = panelLeft + 18;
        int listTop = panelTop + 42;
        int listWidth = Math.min(270, panelWidth / 2 - 22);

        drawString(fontRenderer, "STATS", listLeft + 8, listTop, AMBER_MUTED);
        drawString(fontRenderer, "VALUE", listLeft + listWidth - 48, listTop, AMBER_MUTED);

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
            drawString(fontRenderer, label, listLeft + 9, y + 4, color);

            String value = Integer.toString(row.value);
            drawString(fontRenderer, value, listLeft + listWidth - 42, y + 4, color);
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

        drawString(fontRenderer, "NOTES", infoLeft + 8, infoTop, AMBER_MUTED);
        drawRect(infoLeft, infoTop + 17, infoRight, infoTop + 18, AMBER_MUTED);

        String heading = selectedRow >= 0 && selectedRow < rows.size()
                ? rows.get(selectedRow).name.toUpperCase()
                : "S.P.E.C.I.A.L";
        drawString(fontRenderer, heading, infoLeft + 8, infoTop + 29, AMBER_BRIGHT);

        String description = selectedRow >= 0 && selectedRow < rows.size()
                ? rows.get(selectedRow).description
                : "Select a stat to see what it does.";

        drawWrappedText(description, infoLeft + 8, infoTop + 49,
                Math.max(90, infoWidth - 16), AMBER);

        int statusY = panelTop + panelHeight - 67;
        if (LSPECIALConfig.showSystemStatusInfo) {
            drawRect(infoLeft, statusY, infoRight, statusY + 1, AMBER_MUTED);
            drawString(fontRenderer, LSPECIALConfig.systemStatusText,
                    infoLeft + 8, statusY + 9, AMBER_MUTED);
            drawString(fontRenderer, LSPECIALConfig.neuralLinkText,
                    infoLeft + 8, statusY + 24, AMBER);
        }
    }

    private void drawWrappedText(String text, int x, int y, int maxWidth, int color) {
        StringBuilder line = new StringBuilder();
        int currentY = y;

        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (fontRenderer.getStringWidth(candidate) > maxWidth && line.length() > 0) {
                drawString(fontRenderer, line.toString(), x, currentY, color);
                currentY += 12;
                line.setLength(0);
                line.append(word);
            } else {
                if (line.length() > 0) {
                    line.append(' ');
                }
                line.append(word);
            }
        }

        if (line.length() > 0) {
            drawString(fontRenderer, line.toString(), x, currentY, color);
        }
    }

    private void drawFooter() {
        int footerY = panelTop + panelHeight - 23;
        if (LSPECIALConfig.showRandomFooter && footerMessage != null && !footerMessage.isEmpty()) {
            drawRect(panelLeft + 18, footerY - 5, panelLeft + panelWidth - 18, footerY - 4, AMBER_MUTED);
            drawString(fontRenderer, "LSPECIAL  /  " + footerMessage,
                    panelLeft + 22, footerY + 2, AMBER_MUTED);
        }

        String hint = "PRESS ESC TO CLOSE";

        drawString(fontRenderer, hint, panelLeft + panelWidth - fontRenderer.getStringWidth(hint) - 22, footerY + 2, AMBER);
    }

    private static final class StatRow {
        private final String name;
        private final int value;
        private final boolean enabled;
        private final String description;

        private StatRow(String name, int value, boolean enabled, String description) {
            this.name = name;
            this.value = value;
            this.enabled = enabled;
            this.description = description;
        }
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