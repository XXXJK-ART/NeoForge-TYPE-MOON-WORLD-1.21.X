package com.example.typemoonaddon.client;

import com.example.typemoonaddon.network.OpenDetectionWormControlPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.NeonButton;

public final class DetectionWormControlScreen extends Screen {
    private static final int WINDOW_WIDTH = 290;
    private static final int WINDOW_HEIGHT = 210;

    private final int entityId;
    private final String displayName;
    private final int guPower;
    private final boolean controlled;
    private final boolean sharingVision;
    private final List<String> modeHints;
    private int selectedMode = 0;

    public DetectionWormControlScreen(OpenDetectionWormControlPayload payload) {
        super(Component.translatable("gui.typemoonworld.detection_worm_control.title"));
        this.entityId = payload.entityId();
        this.displayName = payload.displayName();
        this.guPower = payload.guPower();
        this.controlled = payload.controlled();
        this.sharingVision = payload.sharingVision();
        this.modeHints = new ArrayList<>(payload.modeHints());
    }

    @Override
    protected void init() {
        int left = width / 2 - WINDOW_WIDTH / 2;
        int top = height / 2 - WINDOW_HEIGHT / 2;
        addRenderableWidget(new NeonButton(left + 16, top + 46, 108, 20,
                Component.translatable("gui.typemoonworld.detection_worm_control.follow"), b -> selectedMode = 0, GuiUtils.ARCANE_GOLD)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(left + 136, top + 46, 108, 20,
                Component.translatable("gui.typemoonworld.detection_worm_control.recall"), b -> selectedMode = 1, GuiUtils.ARCANE_CYAN)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(left + 16, top + 74, 108, 20,
                Component.translatable("gui.typemoonworld.detection_worm_control.free"), b -> selectedMode = 2, GuiUtils.ARCANE_VALID)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(left + 136, top + 74, 108, 20,
                Component.translatable("gui.typemoonworld.detection_worm_control.attack"), b -> selectedMode = 3, GuiUtils.ARCANE_DANGER)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(left + 16, top + 146, 258, 20,
                Component.translatable("gui.done"), this::save, GuiUtils.ARCANE_GOLD)
                .setArcaneStyle(true).setCompactStyle(true));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.renderScreenBackdrop(graphics, width, height);
        int left = width / 2 - WINDOW_WIDTH / 2;
        int top = height / 2 - WINDOW_HEIGHT / 2;
        GuiUtils.renderArcaneWindow(graphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, GuiUtils.ARCANE_CYAN);
        graphics.drawCenteredString(font, title, width / 2, top + 9, GuiUtils.ARCANE_TEXT);
        graphics.drawString(font, Component.literal(displayName), left + 16, top + 28, GuiUtils.ARCANE_TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable("gui.typemoonworld.detection_worm_control.status",
                controlled ? Component.translatable("gui.typemoonworld.detection_worm_control.controlled") : Component.translatable("gui.typemoonworld.detection_worm_control.free_state"),
                sharingVision ? Component.translatable("gui.typemoonworld.detection_worm_control.shared") : Component.translatable("gui.typemoonworld.detection_worm_control.private_state")),
                left + 16, top + 104, GuiUtils.ARCANE_TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable("gui.typemoonworld.detection_worm_control.gu", guPower), left + 16, top + 120, GuiUtils.ARCANE_TEXT, false);
        for (int i = 0; i < Math.min(4, modeHints.size()); i++) {
            graphics.drawString(font, Component.literal(modeHints.get(i)), left + 16, top + 122 + i * 10, GuiUtils.ARCANE_TEXT_MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void save(Button ignored) {
        PacketDistributor.sendToServer(new com.example.typemoonaddon.network.SetDetectionWormControlPayload(entityId, selectedMode));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
