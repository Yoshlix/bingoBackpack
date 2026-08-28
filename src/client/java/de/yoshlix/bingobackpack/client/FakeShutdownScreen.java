package de.yoshlix.bingobackpack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A dismissible full-screen overlay that imitates the Windows "shutting down"
 * screen. Cosmetic only — a click or ESC closes it. Nothing is actually shut
 * down; this is the harmless stand-in for a real shutdown.
 */
public class FakeShutdownScreen extends Screen {

    private static final int WIN_BLUE = 0xFF0078D7;
    private static final String[] SPINNER = {"|", "/", "-", "\\"};

    private int frame;

    public FakeShutdownScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Shutdown"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void tick() {
        frame++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Skip the default dirt/blur background; our fill covers the screen.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        graphics.fill(0, 0, w, h, WIN_BLUE);

        String spinner = SPINNER[(frame / 5) % SPINNER.length];
        graphics.centeredText(this.font, Component.literal(spinner), w / 2, h / 2 - 30, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal("Wird heruntergefahren"),
                w / 2, h / 2, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal("Schalte den Computer nicht aus."),
                w / 2, h / 2 + 14, 0xFFDDDDDD);
        graphics.centeredText(this.font,
                Component.literal("§7[ Klicke oder drücke ESC zum Schließen ]"),
                w / 2, h - 24, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.onClose();
        return true;
    }
}
