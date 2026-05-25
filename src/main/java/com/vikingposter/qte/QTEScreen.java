package com.vikingposter.qte;

import com.vikingposter.network.C2SQTEResultPacket;
import com.vikingposter.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class QTEScreen extends Screen {

    // Difficulty curve indexed by CURRENT degradation level (the one being incremented from).
    // Level 0→1 is the easiest; 4→5 should feel challenging without being unfair.
    private static final int[]  SEQ_LEN_BY_LEVEL  = { 5, 6, 7, 8, 10 };
    private static final long[] TIMEOUT_BY_LEVEL  = { 8_000L, 7_000L, 6_000L, 5_500L, 5_000L };

    private final BlockPos pos;
    private final int      sequenceLength;
    private final long     timeoutMs;
    private final int      currentLevel;

    private final List<Integer> sequence = new ArrayList<>(); // key codes (A=65...Z=90)
    private int  currentIndex = 0;
    private long startTime;
    private boolean finished = false;
    private boolean success  = false;

    private long wrongKeyTime = -1;

    public QTEScreen(BlockPos pos, int currentLevel) {
        super(Component.literal("QTE Viking"));
        this.pos          = pos;
        this.currentLevel = Math.max(0, Math.min(SEQ_LEN_BY_LEVEL.length - 1, currentLevel));
        this.sequenceLength = SEQ_LEN_BY_LEVEL[this.currentLevel];
        this.timeoutMs      = TIMEOUT_BY_LEVEL[this.currentLevel];
        generateSequence();
    }

    private void generateSequence() {
        Random rng = new Random();
        sequence.clear();
        for (int i = 0; i < sequenceLength; i++) {
            sequence.add(65 + rng.nextInt(26));
        }
    }

    @Override
    protected void init() {
        super.init();
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (finished) return;

        long now     = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed >= timeoutMs && !finished) {
            finish(false);
            return;
        }

        // Compute box size & background width dynamically so the key sequence always fits
        // on screen — especially at high GUI scales (3, 4) where the effective width shrinks.
        int gap = 6;
        int idealBoxSize = 28;
        int availableW = this.width - 60; // margin on each side of the screen
        int spaceForBoxes = availableW - gap * (sequenceLength - 1);
        int boxSize = Math.max(14, Math.min(idealBoxSize, spaceForBoxes / Math.max(1, sequenceLength)));
        int totalW = sequenceLength * (boxSize + gap) - gap;

        int bgW = Math.max(220, totalW + 40);
        int bgH = 180;
        int bgX = this.width / 2 - bgW / 2;
        int bgY = this.height / 2 - bgH / 2;

        // Dark parchment background
        g.fill(bgX,         bgY,         bgX + bgW,     bgY + bgH,     0xDD1A1008);
        g.fill(bgX + 2,     bgY + 2,     bgX + bgW - 2, bgY + bgH - 2, 0xDD2E1E0A);

        // Border
        g.fill(bgX,           bgY,           bgX + bgW,     bgY + 2,         0xFFAA8844);
        g.fill(bgX,           bgY + bgH - 2, bgX + bgW,     bgY + bgH,       0xFFAA8844);
        g.fill(bgX,           bgY,           bgX + 2,       bgY + bgH,       0xFFAA8844);
        g.fill(bgX + bgW - 2, bgY,           bgX + bgW,     bgY + bgH,       0xFFAA8844);

        // Title
        String subtitle = "§6§l~ Vandalisme Viking ~ §7§o(palier " + (currentLevel + 1) + "/5)";
        g.drawCenteredString(font, subtitle, this.width / 2, bgY + 12, 0xFFFFFF);

        // Instruction
        String instr = "Appuyez sur ces runes dans l'ordre :";
        g.drawCenteredString(font, "§e" + instr, this.width / 2, bgY + 32, 0xFFFFFF);

        // Key boxes
        int startX = this.width / 2 - totalW / 2;
        int boxY   = bgY + 55;

        boolean wrongFlash = (wrongKeyTime > 0 && now - wrongKeyTime < 300);

        for (int i = 0; i < sequenceLength; i++) {
            int bx = startX + i * (boxSize + gap);
            int col;
            if (i < currentIndex) {
                col = 0xFF44AA44; // done = green
            } else if (i == currentIndex) {
                col = wrongFlash ? 0xFFAA3333 : 0xFFAA8800; // current = amber or red flash
            } else {
                col = 0xFF553311; // pending = dark
            }
            g.fill(bx, boxY, bx + boxSize, boxY + boxSize, col);
            g.fill(bx+1, boxY+1, bx+boxSize-1, boxY+boxSize-1, col & 0x80FFFFFF | 0x1A000000);

            // Use the localized key name so AZERTY/QWERTZ players see the letter printed on
            // their physical key (GLFW keyCodes are QWERTY-positional).
            String localized = org.lwjgl.glfw.GLFW.glfwGetKeyName(sequence.get(i), 0);
            String keyStr = (localized != null && !localized.isEmpty())
                ? localized.toUpperCase()
                : String.valueOf((char) sequence.get(i).intValue());
            int textColor = (i < currentIndex) ? 0xAAFFAA : (i == currentIndex ? 0xFFDD44 : 0x886644);
            g.drawCenteredString(font, "§l" + keyStr, bx + boxSize/2, boxY + (boxSize - font.lineHeight)/2, textColor);
        }

        // Timer bar
        float progress = 1f - (float)elapsed / timeoutMs;
        int barW    = 200;
        int barX    = this.width / 2 - barW / 2;
        int barY    = bgY + 100;
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF333333);
        int fillW = (int)(barW * progress);
        int barCol = progress > 0.5f ? 0xFF44BB44 : (progress > 0.25f ? 0xFFBBAA22 : 0xFFBB3322);
        g.fill(barX, barY, barX + fillW, barY + 8, barCol);
        g.fill(barX, barY, barX + barW, barY + 1, 0xFF555555);

        String timerStr = String.format("%.1fs", (timeoutMs - elapsed) / 1000.0);
        g.drawCenteredString(font, "§7" + timerStr, this.width / 2, barY + 12, 0xFFFFFF);

        // Progress text
        String prog = currentIndex + " / " + sequenceLength;
        g.drawCenteredString(font, "§7" + prog, this.width / 2, bgY + 148, 0xFFFFFF);

        // Hint: ESC to cancel
        g.drawCenteredString(font, "§8[Echap] Annuler", this.width / 2, bgY + 163, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESCAPE
            finish(false);
            return true;
        }
        if (finished) return true;

        // Check if pressed key matches current expected key
        // GLFW key codes: A=65, B=66, ..., Z=90
        if (keyCode >= 65 && keyCode <= 90) {
            if (keyCode == sequence.get(currentIndex)) {
                currentIndex++;
                if (currentIndex >= sequenceLength) {
                    finish(true);
                }
            } else {
                // Wrong key: flash and reset
                wrongKeyTime = System.currentTimeMillis();
                currentIndex = 0;
            }
        }
        return true;
    }

    private void finish(boolean won) {
        if (finished) return;
        finished = true;
        success  = won;
        NetworkHandler.sendToServer(new C2SQTEResultPacket(pos, won));
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; } // handle ESC ourselves
}
