package com.example.typemoonaddon.client;

import com.example.typemoonaddon.engravedworm.EngravedWormMenu;
import com.example.typemoonaddon.engravedworm.EngravedWormPagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public final class EngravedWormScreen extends AbstractContainerScreen<EngravedWormMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public EngravedWormScreen(EngravedWormMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(menu.getPage() - 1))
                .bounds(leftPos + 6, topPos + 4, 18, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(menu.getPage() + 1))
                .bounds(leftPos + 28, topPos + 4, 18, 14).build());
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
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, 71);
        graphics.blit(BACKGROUND, leftPos, topPos + 71, 0, 126, imageWidth, 96);
        graphics.drawString(font, Component.literal("P" + (menu.getPage() + 1) + "/" + (menu.getMaxPage() + 1)),
                leftPos + 54, topPos + 6, 0x404040, false);
    }
}
