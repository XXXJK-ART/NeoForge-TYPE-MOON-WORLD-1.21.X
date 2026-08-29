package io.github.typemoonaddon.client;

import io.github.typemoonaddon.menu.ImaginarySpaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ImaginarySpaceScreen extends AbstractContainerScreen<ImaginarySpaceMenu> {
    private static final int PANEL_COLOR_TOP = 0xF0181028;
    private static final int PANEL_COLOR_BOTTOM = 0xF0060612;
    private static final int BORDER_COLOR = 0xFF9B6BC6;
    private static final int SLOT_COLOR = 0xB0100A1B;

    private Button previousPage;
    private Button nextPage;

    public ImaginarySpaceScreen(ImaginarySpaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        previousPage = addRenderableWidget(Button.builder(
            Component.literal("<"),
            ignored -> requestPage(ImaginarySpaceMenu.PREVIOUS_PAGE_BUTTON)
        ).bounds(leftPos + 104, topPos + 122, 18, 14).build());
        nextPage = addRenderableWidget(Button.builder(
            Component.literal(">"),
            ignored -> requestPage(ImaginarySpaceMenu.NEXT_PAGE_BUTTON)
        ).bounds(leftPos + 151, topPos + 122, 18, 14).build());
        updatePageButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fillGradient(left, top, left + imageWidth, top + imageHeight, PANEL_COLOR_TOP, PANEL_COLOR_BOTTOM);
        graphics.fill(left, top, left + imageWidth, top + 1, BORDER_COLOR);
        graphics.fill(left, top + imageHeight - 1, left + imageWidth, top + imageHeight, BORDER_COLOR);
        graphics.fill(left, top, left + 1, top + imageHeight, BORDER_COLOR);
        graphics.fill(left + imageWidth - 1, top, left + imageWidth, top + imageHeight, BORDER_COLOR);

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                int x = left + 7 + column * 18;
                int y = top + 17 + row * 18;
                graphics.fill(x, y, x + 18, y + 18, 0x80452A62);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
            }
        }
        for (int row = 0; row < 4; row++) {
            int columns = 9;
            for (int column = 0; column < columns; column++) {
                int x = left + 7 + column * 18;
                int y = top + 139 + row * 18 + (row == 3 ? 4 : 0);
                graphics.fill(x, y, x + 18, y + 18, 0x60452A62);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xDDBBFF, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xBFA6D8, false);
        Component page = Component.translatable(
            "menu.typemoonaddon.imaginary_space.page",
            menu.currentPage() + 1,
            menu.pageCount()
        );
        graphics.drawCenteredString(font, page, 136, 125, 0xDDBBFF);
    }

    private void requestPage(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private void updatePageButtons() {
        if (previousPage != null) {
            previousPage.active = menu.currentPage() > 0;
        }
        if (nextPage != null) {
            nextPage.active = menu.currentPage() + 1 < menu.pageCount();
        }
    }
}
