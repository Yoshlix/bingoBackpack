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
 * It can only be dismissed by solving a small random challenge.
 */
public class FakeShutdownScreen extends Screen {

    private static final int WIN_BLUE = 0xFF0078D7;
    private static final String[] SPINNER = {"|", "/", "-", "\\"};
    private static final String[] TYPE_WORDS =
            {"WARTEN", "GEDULD", "UPDATE", "NEUSTART", "FEHLER", "SYSTEM"};

    private int frame;
    private final Challenge challenge = randomChallenge();
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
        int boxWidth = Math.max(110, font.width(challenge.answer()) + 60);
        answerBox = addRenderableWidget(new EditBox(font, width / 2 - boxWidth / 2,
                height / 2 + 20, boxWidth, 20, Component.literal("Antwort")));
        answerBox.setMaxLength(challenge.answer().length() + 2);
        setInitialFocus(answerBox);
        addRenderableWidget(Button.builder(Component.literal("Bestätigen"), button -> checkAnswer())
                .bounds(width / 2 - 55, height / 2 + 46, 110, 20)
                .build());
    }

    private void checkAnswer() {
        if (answerBox.getValue().trim().equalsIgnoreCase(challenge.answer())) {
            onClose();
            return;
        }
        feedback = "Falsch. Versuch's noch mal.";
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
        graphics.centeredText(this.font, Component.literal(spinner), w / 2, h / 2 - 50, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal("Wird heruntergefahren"),
                w / 2, h / 2 - 30, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal("Schalte den Computer nicht aus."),
                w / 2, h / 2 - 14, 0xFFDDDDDD);
        graphics.centeredText(this.font, Component.literal(challenge.question()),
                w / 2, h / 2 + 6, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.literal("§7" + feedback), w / 2, h / 2 + 74, 0xFFDDDDDD);

        // Screen renders registered widgets (EditBox and Button) in its base
        // implementation. This must run after the blue background.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    // ------------------------------------------------------------------ challenges

    private record Challenge(String question, String answer) {
    }

    private static Challenge randomChallenge() {
        return switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> additionChallenge();
            case 1 -> subtractionChallenge();
            case 2 -> multiplicationChallenge();
            default -> wordChallenge();
        };
    }

    private static Challenge additionChallenge() {
        int a = ThreadLocalRandom.current().nextInt(10, 91);
        int b = ThreadLocalRandom.current().nextInt(10, 91);
        return new Challenge(a + " + " + b + " = ?", String.valueOf(a + b));
    }

    private static Challenge subtractionChallenge() {
        int a = ThreadLocalRandom.current().nextInt(20, 99);
        int b = ThreadLocalRandom.current().nextInt(1, a);
        return new Challenge(a + " - " + b + " = ?", String.valueOf(a - b));
    }

    private static Challenge multiplicationChallenge() {
        int a = ThreadLocalRandom.current().nextInt(2, 13);
        int b = ThreadLocalRandom.current().nextInt(2, 13);
        return new Challenge(a + " x " + b + " = ?", String.valueOf(a * b));
    }

    private static Challenge wordChallenge() {
        String word = TYPE_WORDS[ThreadLocalRandom.current().nextInt(TYPE_WORDS.length)];
        return new Challenge("Tippe zur Bestätigung: " + word, word);
    }
}
