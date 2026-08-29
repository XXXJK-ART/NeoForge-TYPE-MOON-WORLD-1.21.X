package io.github.typemoonaddon.client;

import io.github.typemoonaddon.network.OpenDevourerSelectionPayload;
import io.github.typemoonaddon.network.SetDevourerSelectionPayload;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HeroicSpiritDevourerScreen extends Screen {
    private static final int WINDOW_WIDTH = 300;
    private static final int WINDOW_HEIGHT = 230;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 7;
    private static final int ACCENT = 0xFFD34A4A;
    private static final int TEXT = 0xFFF0E8E8;
    private static final int MUTED_TEXT = 0xFF9A8589;
    private static final int VALID = 0xFF49C98A;
    private static final int GOLD = 0xFFE2B85B;
    private static final int CYAN = 0xFF53D5DE;

    private final List<OpenDevourerSelectionPayload.Entry> entries;
    private final Set<UUID> selected = new HashSet<>();
    private final boolean loading;
    private int scrollOffset;
    private int focusedIndex;

    public HeroicSpiritDevourerScreen(OpenDevourerSelectionPayload payload) {
        this(payload.entries(), false);
    }

    private HeroicSpiritDevourerScreen(
        List<OpenDevourerSelectionPayload.Entry> entries,
        boolean loading
    ) {
        super(Component.translatable("screen.typemoonaddon.heroic_spirit_devourer.title"));
        this.entries = List.copyOf(entries);
        this.loading = loading;
        for (OpenDevourerSelectionPayload.Entry entry : entries) {
            if (entry.selected()) {
                selected.add(entry.rosterId());
            }
        }
    }

    public static HeroicSpiritDevourerScreen loading() {
        return new HeroicSpiritDevourerScreen(List.of(), true);
    }

    @Override
    protected void init() {
        int left = width / 2 - WINDOW_WIDTH / 2;
        int top = height / 2 - WINDOW_HEIGHT / 2;
        Button done = addRenderableWidget(Button.builder(Component.translatable("gui.done"), this::save)
            .bounds(left + 20, top + WINDOW_HEIGHT - 31, WINDOW_WIDTH - 40, 20)
            .build());
        done.active = !loading;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0080508);
        int left = width / 2 - WINDOW_WIDTH / 2;
        int top = height / 2 - WINDOW_HEIGHT / 2;
        int listX = left + 15;
        int listY = top + 43;
        int listWidth = WINDOW_WIDTH - 30;
        graphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xF0120B0E);
        graphics.renderOutline(left, top, WINDOW_WIDTH, WINDOW_HEIGHT, ACCENT);
        graphics.fill(listX, listY, listX + listWidth, listY + VISIBLE_ROWS * ROW_HEIGHT, 0xE0080608);
        graphics.renderOutline(listX, listY, listWidth, VISIBLE_ROWS * ROW_HEIGHT, 0xFF6E2733);
        graphics.drawCenteredString(font, title, width / 2, top + 9, TEXT);
        graphics.drawCenteredString(
            font,
            Component.translatable("screen.typemoonaddon.heroic_spirit_devourer.selected", selected.size()),
            width / 2,
            top + 26,
            MUTED_TEXT
        );

        int end = Math.min(entries.size(), scrollOffset + VISIBLE_ROWS);
        for (int index = scrollOffset; index < end; index++) {
            renderEntry(graphics, entries.get(index), index, listX, listY + (index - scrollOffset) * ROW_HEIGHT, listWidth);
        }
        if (loading || entries.isEmpty()) {
            graphics.drawCenteredString(
                font,
                Component.translatable(
                    loading
                        ? "screen.typemoonaddon.heroic_spirit_devourer.loading"
                        : "screen.typemoonaddon.heroic_spirit_devourer.empty"
                ),
                width / 2,
                listY + VISIBLE_ROWS * ROW_HEIGHT / 2 - 4,
                MUTED_TEXT
            );
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderEntry(
        GuiGraphics graphics,
        OpenDevourerSelectionPayload.Entry entry,
        int index,
        int x,
        int y,
        int width
    ) {
        boolean focused = index == focusedIndex;
        if (focused) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + ROW_HEIGHT - 1, 0x402F1018);
        }
        int boxX = x + 7;
        int boxY = y + 5;
        graphics.renderOutline(boxX, boxY, 10, 10, focused ? GOLD : ACCENT);
        if (selected.contains(entry.rosterId())) {
            graphics.fill(boxX + 2, boxY + 2, boxX + 8, boxY + 8, CYAN);
        }
        Component state = Component.translatable(
            "screen.typemoonaddon.heroic_spirit_devourer." + (entry.active() ? "active" : "stored")
        );
        Component detail = entry.player()
            ? Component.empty()
                .append(Component.translatable("screen.typemoonaddon.heroic_spirit_devourer.player"))
                .append(" / ")
                .append(state)
            : state;
        int detailWidth = font.width(detail);
        String name = font.plainSubstrByWidth(entry.displayName(), width - detailWidth - 42);
        graphics.drawString(font, name, x + 23, y + 6, TEXT, false);
        int stateColor = entry.active() ? VALID : MUTED_TEXT;
        graphics.drawString(font, detail, x + width - 8 - detailWidth, y + 6, stateColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (loading) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            int left = width / 2 - WINDOW_WIDTH / 2;
            int top = height / 2 - WINDOW_HEIGHT / 2;
            int listX = left + 15;
            int listY = top + 43;
            if (mouseX >= listX && mouseX < listX + WINDOW_WIDTH - 30
                && mouseY >= listY && mouseY < listY + VISIBLE_ROWS * ROW_HEIGHT) {
                int index = scrollOffset + (int)((mouseY - listY) / ROW_HEIGHT);
                if (index < entries.size()) {
                    focusedIndex = index;
                    toggle(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D && entries.size() > VISIBLE_ROWS) {
            scrollOffset = Math.clamp(
                scrollOffset + (scrollY > 0.0D ? -1 : 1),
                0,
                entries.size() - VISIBLE_ROWS
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!entries.isEmpty() && (keyCode == 264 || keyCode == 262)) {
            focusedIndex = Math.min(entries.size() - 1, focusedIndex + 1);
            keepFocusedVisible();
            return true;
        }
        if (!entries.isEmpty() && (keyCode == 265 || keyCode == 263)) {
            focusedIndex = Math.max(0, focusedIndex - 1);
            keepFocusedVisible();
            return true;
        }
        if (!entries.isEmpty() && keyCode == 32) {
            toggle(focusedIndex);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void keepFocusedVisible() {
        if (focusedIndex < scrollOffset) {
            scrollOffset = focusedIndex;
        } else if (focusedIndex >= scrollOffset + VISIBLE_ROWS) {
            scrollOffset = focusedIndex - VISIBLE_ROWS + 1;
        }
    }

    private void toggle(int index) {
        UUID rosterId = entries.get(index).rosterId();
        if (!selected.remove(rosterId)) {
            selected.add(rosterId);
        }
    }

    private void save(Button ignored) {
        if (loading) {
            return;
        }
        PacketDistributor.sendToServer(new SetDevourerSelectionPayload(List.copyOf(selected)));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
