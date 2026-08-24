package com.example.typemoonaddon.client;

import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import com.example.typemoonaddon.network.OpenBoundaryImmunityPayload;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.NeonButton;

public final class BoundaryImmunityScreen extends Screen {
    private static final int WINDOW_WIDTH = 320;
    private static final int WINDOW_HEIGHT = 236;
    private static final int ROW_HEIGHT = 20;

    private final int entityId;
    private final String entityName;
    private final List<ResourceLocation> boundaryIds = new ArrayList<>(BoundaryMagicIntegration.boundaryMagicIds());
    private final Set<String> selected = new LinkedHashSet<>();

    public BoundaryImmunityScreen(OpenBoundaryImmunityPayload payload) {
        super(Component.translatable("gui.typemoonworld.boundary_immunity.title"));
        this.entityId = payload.entityId();
        this.entityName = payload.displayName();
        this.selected.addAll(payload.selectedBoundaryIds());
    }

    @Override
    protected void init() {
        int left = width / 2 - WINDOW_WIDTH / 2;
        int top = height / 2 - WINDOW_HEIGHT / 2;
        addRenderableWidget(new NeonButton(left + 18, top + WINDOW_HEIGHT - 28, 78, 20,
                Component.translatable("gui.typemoonworld.boundary_immunity.all"), button -> {
                    selected.clear();
                    for (ResourceLocation id : boundaryIds) {
                        selected.add(id.toString());
                    }
                }, GuiUtils.ARCANE_VALID).setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(left + 102, top + WINDOW_HEIGHT - 28, 78, 20,
                Component.translatable("gui.typemoonworld.boundary_immunity.none"), button -> selected.clear(),
                GuiUtils.ARCANE_DANGER).setArcaneStyle(true).setCompactStyle(true));
        addRenderableWidget(new NeonButton(left + 186, top + WINDOW_HEIGHT - 28, 96, 20,
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
        graphics.drawString(font, Component.literal(entityName), left + 16, top + 28, GuiUtils.ARCANE_TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable("gui.typemoonworld.boundary_immunity.count", selected.size()),
                left + WINDOW_WIDTH - 12 - font.width(Component.translatable("gui.typemoonworld.boundary_immunity.count", selected.size())),
                top + 28, GuiUtils.ARCANE_TEXT_MUTED, false);
        for (int index = 0; index < boundaryIds.size(); index++) {
            ResourceLocation id = boundaryIds.get(index);
            boolean checked = selected.contains(id.toString());
            int col = index % 2;
            int row = index / 2;
            int x = left + 16 + col * 144;
            int y = top + 52 + row * ROW_HEIGHT;
            graphics.fill(x, y, x + 134, y + 16, checked ? 0x4032A06D : 0x30202020);
            graphics.drawString(font, labelFor(id), x + 8, y + 4, GuiUtils.ARCANE_TEXT, false);
            graphics.drawString(font, checked ? Component.translatable("gui.typemoonworld.boundary_immunity.enabled")
                    : Component.translatable("gui.typemoonworld.boundary_immunity.disabled"),
                    x + 92, y + 4, checked ? GuiUtils.ARCANE_VALID : GuiUtils.ARCANE_TEXT_MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = width / 2 - WINDOW_WIDTH / 2;
            int top = height / 2 - WINDOW_HEIGHT / 2;
            for (int index = 0; index < boundaryIds.size(); index++) {
                ResourceLocation id = boundaryIds.get(index);
                int col = index % 2;
                int row = index / 2;
                int x = left + 16 + col * 144;
                int y = top + 52 + row * ROW_HEIGHT;
                if (mouseX >= x && mouseX < x + 134 && mouseY >= y && mouseY < y + 16) {
                    String key = id.toString();
                    if (!selected.remove(key)) {
                        selected.add(key);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void save(Button ignored) {
        PacketDistributor.sendToServer(new com.example.typemoonaddon.network.SetBoundaryImmunityPayload(entityId, List.copyOf(selected)));
        onClose();
    }

    private static Component labelFor(ResourceLocation id) {
        return Component.translatable("magic.typemoonworld." + id.getPath() + ".name");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
