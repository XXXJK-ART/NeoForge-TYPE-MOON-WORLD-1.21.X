package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import net.xxxjk.TYPE_MOON_WORLD.network.GemGravitySelfCastMessage;
import org.jetbrains.annotations.NotNull;

public class GemGravityModeSelectScreen extends Screen {
    private final int handValue;

    public GemGravityModeSelectScreen(InteractionHand hand) {
        super(Component.translatable("gui.typemoonworld.gem.gravity_select.title"));
        this.handValue = hand == InteractionHand.OFF_HAND ? 1 : 0;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 170;
        int buttonHeight = 20;
        int gap = 4;
        int totalHeight = (buttonHeight * 5) + (gap * 4);
        int startX = (this.width - buttonWidth) / 2;
        int startY = (this.height - totalHeight) / 2;

        addModeButton(startX, startY, buttonWidth, buttonHeight, MagicGravity.MODE_ULTRA_LIGHT, "gui.typemoonworld.mode.gravity.ultra_light");
        addModeButton(startX, startY + (buttonHeight + gap), buttonWidth, buttonHeight, MagicGravity.MODE_LIGHT, "gui.typemoonworld.mode.gravity.light");
        addModeButton(startX, startY + (buttonHeight + gap) * 2, buttonWidth, buttonHeight, MagicGravity.MODE_NORMAL, "gui.typemoonworld.mode.gravity.normal");
        addModeButton(startX, startY + (buttonHeight + gap) * 3, buttonWidth, buttonHeight, MagicGravity.MODE_HEAVY, "gui.typemoonworld.mode.gravity.heavy");
        addModeButton(startX, startY + (buttonHeight + gap) * 4, buttonWidth, buttonHeight, MagicGravity.MODE_ULTRA_HEAVY, "gui.typemoonworld.mode.gravity.ultra_heavy");
    }

    private void addModeButton(int x, int y, int width, int height, int mode, String modeKey) {
        this.addRenderableWidget(Button.builder(Component.translatable(modeKey), btn -> {
            PacketDistributor.sendToServer(new GemGravitySelfCastMessage(this.handValue, mode));
            this.onClose();
        }).bounds(x, y, width, height).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, (this.height / 2) - 66, 0xFFFFFF);
    }
}
