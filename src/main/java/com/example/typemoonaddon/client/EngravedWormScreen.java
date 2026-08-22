package com.example.typemoonaddon.client;

import com.example.typemoonaddon.engravedworm.EngravedWormMenu;
import com.example.typemoonaddon.engravedworm.EngravedWormPagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.NeonButton;
import org.jetbrains.annotations.NotNull;

public final class EngravedWormScreen extends AbstractContainerScreen<EngravedWormMenu> {
    private static final int WINDOW_COLOR = GuiUtils.ARCANE_CREST;

    public EngravedWormScreen(EngravedWormMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = topPos + 5;
        addRenderableWidget(new NeonButton(leftPos + 6, buttonY, 18, 14, Component.literal("<"), b -> changePage(menu.getPage() - 1), GuiUtils.ARCANE_CREST)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(leftPos + 26, buttonY, 18, 14, Component.literal(">"), b -> changePage(menu.getPage() + 1), GuiUtils.ARCANE_CYAN)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(leftPos + 152, buttonY, 18, 14, Component.literal("<<"), b -> changePage(0), GuiUtils.ARCANE_GOLD)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(leftPos + 172, buttonY, 18, 14, Component.literal(">>"), b -> changePage(menu.getMaxPage()), GuiUtils.ARCANE_GOLD)
                .setArcaneStyle(true).setCompactStyle(true));
    }

    private void changePage(int page) {
        int next = Math.max(0, Math.min(menu.getMaxPage(), page));
        menu.setPage(next);
        PacketDistributor.sendToServer(new EngravedWormPagePayload(next), new CustomPacketPayload[0]);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiUtils.renderScreenBackdrop(graphics, width, height);
        GuiUtils.renderArcaneWindow(graphics, leftPos, topPos, imageWidth, imageHeight, WINDOW_COLOR);
        graphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 7, GuiUtils.ARCANE_TEXT);
        graphics.drawCenteredString(font, Component.literal("P" + (menu.getPage() + 1) + "/" + (menu.getMaxPage() + 1)),
               leftPos + imageWidth / 2, topPos + 19, GuiUtils.ARCANE_TEXT_MUTED);
        GuiUtils.renderSectionHeader(graphics, leftPos + 8, topPos + 32, imageWidth - 16, GuiUtils.ARCANE_CREST);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                GuiUtils.renderArcaneSlot(graphics, leftPos + 8 + column * 18, topPos + 44 + row * 18, 18, GuiUtils.ARCANE_CREST, false);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                GuiUtils.renderArcaneSlot(graphics, leftPos + 8 + column * 18, topPos + 130 + row * 18, 18, GuiUtils.ARCANE_CYAN, false);
            }
        }
        for (int column = 0; column < 9; column++) {
            GuiUtils.renderArcaneSlot(graphics, leftPos + 8 + column * 18, topPos + 188, 18, GuiUtils.ARCANE_GOLD, false);
        }
    }
}
