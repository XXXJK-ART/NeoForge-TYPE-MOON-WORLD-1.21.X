package com.example.typemoonaddon.client;

import com.example.typemoonaddon.worm.WormWarehouseService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.example.typemoonaddon.network.WormWarehousePageMessage;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.WormWarehouseMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public final class WormWarehouseScreen extends AbstractContainerScreen<WormWarehouseMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public WormWarehouseScreen(WormWarehouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = topPos + 4;
        addRenderableWidget(Button.builder(Component.literal("|<"), button -> {
            menu.setPage(0);
            PacketDistributor.sendToServer(new WormWarehousePageMessage(0), new CustomPacketPayload[0]);
        }).bounds(leftPos + 6, buttonY, 18, 14).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            int next = Math.max(0, menu.getPage() - 1);
            menu.setPage(next);
            PacketDistributor.sendToServer(new WormWarehousePageMessage(next), new CustomPacketPayload[0]);
        }).bounds(leftPos + 26, buttonY, 18, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            int next = Math.min(menu.getMaxPage(), menu.getPage() + 1);
            menu.setPage(next);
            PacketDistributor.sendToServer(new WormWarehousePageMessage(next), new CustomPacketPayload[0]);
        }).bounds(leftPos + 142, buttonY, 18, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">|"), button -> {
            int next = menu.getMaxPage();
            menu.setPage(next);
            PacketDistributor.sendToServer(new WormWarehousePageMessage(next), new CustomPacketPayload[0]);
        }).bounds(leftPos + 162, buttonY, 18, 14).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, left, top, 0, 0, imageWidth, 71);
        graphics.blit(TEXTURE, left, top + 71, 0, 126, imageWidth, 96);
        graphics.drawString(font, Component.literal("P" + (menu.getPage() + 1) + "/" + (menu.getMaxPage() + 1)), left + 54, top + 6, 0x404040, false);
        graphics.drawString(font, Component.literal("Cap " + menu.getOccupiedSlots() + "/" + menu.getCapacity()),
                left + 104, top + 6, 0x404040, false);
        graphics.drawString(font, Component.literal("Start " + (menu.getPage() * WormWarehouseService.PAGE_SIZE + 1)), left + 54, top + 18, 0x606060, false);
    }
}
