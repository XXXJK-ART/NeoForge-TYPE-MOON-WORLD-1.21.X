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
        imageHeight = 272;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = topPos + 24;
        addRenderableWidget(new NeonButton(leftPos + 8, buttonY, 20, 14, Component.literal("<"), b -> changePage(menu.getPage() - 1), GuiUtils.ARCANE_CREST)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(leftPos + 31, buttonY, 20, 14, Component.literal(">"), b -> changePage(menu.getPage() + 1), GuiUtils.ARCANE_CYAN)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(leftPos + 141, buttonY, 23, 14, Component.literal("<<"), b -> changePage(0), GuiUtils.ARCANE_GOLD)
                .setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(leftPos + 166, buttonY, 23, 14, Component.literal(">>"), b -> changePage(menu.getMaxPage()), GuiUtils.ARCANE_GOLD)
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
               leftPos + imageWidth / 2, topPos + 27, GuiUtils.ARCANE_TEXT_MUTED);
        GuiUtils.renderSectionHeader(graphics, leftPos + 8, topPos + 42, imageWidth - 16, GuiUtils.ARCANE_CREST);
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                GuiUtils.renderArcaneSlot(graphics, leftPos + EngravedWormMenu.GRID_X + column * 18,
                        topPos + EngravedWormMenu.WORM_GRID_Y + row * 18, 18, GuiUtils.ARCANE_CREST, false);
            }
        }
        graphics.drawString(font, playerInventoryTitle, leftPos + EngravedWormMenu.GRID_X,
                topPos + EngravedWormMenu.PLAYER_INV_Y - 12, GuiUtils.ARCANE_TEXT_MUTED, false);
        GuiUtils.renderSectionHeader(graphics, leftPos + 8, topPos + EngravedWormMenu.PLAYER_INV_Y - 20,
                imageWidth - 16, GuiUtils.ARCANE_CYAN);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                GuiUtils.renderArcaneSlot(graphics, leftPos + EngravedWormMenu.GRID_X + column * 18,
                        topPos + EngravedWormMenu.PLAYER_INV_Y + row * 18, 18, GuiUtils.ARCANE_CYAN, false);
            }
        }
        for (int column = 0; column < 9; column++) {
            GuiUtils.renderArcaneSlot(graphics, leftPos + EngravedWormMenu.GRID_X + column * 18,
                    topPos + EngravedWormMenu.HOTBAR_Y, 18, GuiUtils.ARCANE_GOLD, false);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
