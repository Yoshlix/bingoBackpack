package de.yoshlix.bingobackpack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A full-screen overlay that imitates the Windows "shutting down" screen.
 * It can only be dismissed by solving the displayed arithmetic question.
 */
public class FakeShutdownScreen extends Screen {

    private static final int WIN_BLUE = 0xFF0078D7;
    private static final String[] SPINNER = {"|", "/", "-", "\\"};

    private int frame;
    private final int left = ThreadLocalRandom.current().nextInt(10, 91);
    private final int right = ThreadLocalRandom.current().nextInt(10, 91);
    private final int answer = left + right;
    private EditBox answerBox;
    private String feedback = "Löse die Aufgabe, um fortzufahren.";

    public FakeShutdownScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Shutdown"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        int boxWidth = 110;
        answerBox = addRenderableWidget(new EditBox(font, width / 2 - boxWidth / 2,
                height / 2 + 32, boxWidth, 20, Component.literal("Antwort")));
        answerBox.setMaxLength(4);
        setInitialFocus(answerBox);
        addRenderableWidget(Button.builder(Component.literal("Bestätigen"), button -> checkAnswer())
                .bounds(width / 2 - 55, height / 2 + 58, 110, 20)
                .build());
    }

    private void checkAnswer() {
        try {
            if (Integer.parseInt(answerBox.getValue()) == answer) {
                onClose();
                return;
            }
        } catch (NumberFormatException ignored) {
            // The feedback below covers empty and non-numeric input.
        }
        feedback = "Falsch. Rechne noch einmal nach.";
        answerBox.setValue("");
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
        graphics.centeredText(this.font, Component.literal(left + " + " + right + " = ?"),
                w / 2, h / 2 + 22, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.literal("§7" + feedback), w / 2, h - 24, 0xFFDDDDDD);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }
}
