package com.example.typemoonaddon.client;

import com.example.typemoonaddon.network.OpenShadowTransferPayload;
import com.example.typemoonaddon.network.SelectShadowTransferPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;

public final class ShadowTransferScreen extends Screen {
    private static final int ACCENT = 0xFFD34A4A;
    private static final int MAP_PANEL_WIDTH = 328;
    private static final int MAP_PANEL_HEIGHT = 210;
    private static final int MAP_PADDING = 13;
    private static final int MARKER_SIZE = 14;

    private final int originX;
    private final int originZ;
    private final List<OpenShadowTransferPayload.Target> targets;
    private int selectedTargetIndex;
    private OpenShadowTransferPayload.Target hoveredTarget;
    private List<Marker> cachedMarkers = List.of();
    private int cachedMapX = Integer.MIN_VALUE;
    private int cachedMapY = Integer.MIN_VALUE;

    public ShadowTransferScreen(OpenShadowTransferPayload payload) {
        super(Component.translatable("screen.typemoonworld.shadow_transfer.title"));
        this.originX = payload.originX();
        this.originZ = payload.originZ();
        this.targets = payload.targets();
        this.selectedTargetIndex = this.targets.isEmpty() ? -1 : 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
        int panelX = (this.width - MAP_PANEL_WIDTH) / 2;
        int panelY = (this.height - MAP_PANEL_HEIGHT) / 2;
        int mapX = panelX + 12;
        int mapY = panelY + 36;
        int mapWidth = MAP_PANEL_WIDTH - 24;
        int mapHeight = MAP_PANEL_HEIGHT - 49;

        GuiUtils.renderArcaneWindow(gui, panelX, panelY, MAP_PANEL_WIDTH, MAP_PANEL_HEIGHT, ACCENT);
        GuiUtils.renderArcanePanel(gui, mapX, mapY, mapWidth, mapHeight, ACCENT);
        gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
        Component count = Component.translatable("screen.typemoonworld.shadow_transfer.target_count", this.targets.size());
        gui.drawString(this.font, count, panelX + MAP_PANEL_WIDTH - 12 - this.font.width(count), panelY + 9, GuiUtils.ARCANE_TEXT_MUTED, false);

        renderMapGrid(gui, mapX, mapY, mapWidth, mapHeight);
        this.hoveredTarget = null;
        if (this.targets.isEmpty()) {
            gui.drawCenteredString(this.font, Component.translatable("screen.typemoonworld.shadow_transfer.no_targets"),
                    mapX + mapWidth / 2, mapY + mapHeight / 2 - 4, GuiUtils.ARCANE_TEXT_MUTED);
        } else {
            renderMarkers(gui, mouseX, mouseY, mapX, mapY, mapWidth, mapHeight);
        }
        super.render(gui, mouseX, mouseY, partialTick);

        if (this.hoveredTarget != null) {
            gui.renderTooltip(this.font, List.of(
                    Component.translatable("screen.typemoonworld.shadow_transfer.coordinates", this.hoveredTarget.x(), this.hoveredTarget.y(), this.hoveredTarget.z()),
                    Component.translatable("screen.typemoonworld.shadow_transfer.cost", this.hoveredTarget.distance(), this.hoveredTarget.manaCost())
            ), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !this.targets.isEmpty()) {
            int panelX = (this.width - MAP_PANEL_WIDTH) / 2;
            int panelY = (this.height - MAP_PANEL_HEIGHT) / 2;
            List<Marker> markers = layoutMarkers(panelX + 12, panelY + 36, MAP_PANEL_WIDTH - 24, MAP_PANEL_HEIGHT - 49);
            for (int index = markers.size() - 1; index >= 0; index--) {
                Marker marker = markers.get(index);
                if (inside(mouseX, mouseY, marker)) {
                    selectTarget(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.targets.isEmpty() && scrollY != 0.0D) {
            this.selectedTargetIndex = Math.floorMod(this.selectedTargetIndex + (scrollY > 0.0D ? -1 : 1), this.targets.size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.targets.isEmpty()) {
            if (keyCode == 262 || keyCode == 264) {
                this.selectedTargetIndex = (this.selectedTargetIndex + 1) % this.targets.size();
                return true;
            }
            if (keyCode == 263 || keyCode == 265) {
                this.selectedTargetIndex = Math.floorMod(this.selectedTargetIndex - 1, this.targets.size());
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                selectTarget(this.selectedTargetIndex);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderMarkers(GuiGraphics gui, int mouseX, int mouseY, int mapX, int mapY, int mapWidth, int mapHeight) {
        List<Marker> markers = layoutMarkers(mapX, mapY, mapWidth, mapHeight);
        for (int index = 0; index < markers.size(); index++) {
            Marker marker = markers.get(index);
            boolean hovered = inside(mouseX, mouseY, marker);
            boolean selected = index == this.selectedTargetIndex;
            int accent = hovered ? GuiUtils.ARCANE_VALID : selected ? GuiUtils.ARCANE_GOLD : ACCENT;
            gui.fill(marker.x, marker.y, marker.x + MARKER_SIZE, marker.y + MARKER_SIZE, hovered || selected ? 0xF02A171B : GuiUtils.ARCANE_PANEL_ALT);
            gui.renderOutline(marker.x, marker.y, MARKER_SIZE, MARKER_SIZE, accent);
            gui.fill(marker.x + 1, marker.y + 1, marker.x + 3, marker.y + MARKER_SIZE - 1, accent);
            gui.fill(marker.x + 5, marker.y + 5, marker.x + 9, marker.y + 9, accent);
            if (hovered) {
                this.hoveredTarget = marker.target;
                this.selectedTargetIndex = index;
            }
        }

        int[] origin = mapPosition(this.originX, this.originZ, mapX, mapY, mapWidth, mapHeight, true);
        gui.fill(origin[0] - 4, origin[1], origin[0] + 5, origin[1] + 1, GuiUtils.ARCANE_TEXT);
        gui.fill(origin[0], origin[1] - 4, origin[0] + 1, origin[1] + 5, GuiUtils.ARCANE_TEXT);
    }

    private List<Marker> layoutMarkers(int mapX, int mapY, int mapWidth, int mapHeight) {
        if (mapX == this.cachedMapX && mapY == this.cachedMapY) {
            return this.cachedMarkers;
        }
        List<Marker> markers = new ArrayList<>(this.targets.size());
        for (OpenShadowTransferPayload.Target target : this.targets) {
            int[] base = mapPosition(target.x(), target.z(), mapX, mapY, mapWidth, mapHeight, false);
            int markerX = base[0];
            int markerY = base[1];
            for (int attempt = 0; attempt < 192; attempt++) {
                if (attempt > 0) {
                    double radius = 4.0D + Math.sqrt(attempt) * 7.0D;
                    double angle = attempt * 2.399963229728653D;
                    markerX = clampMarkerX(base[0] + (int) Math.round(Math.cos(angle) * radius), mapX, mapWidth);
                    markerY = clampMarkerY(base[1] + (int) Math.round(Math.sin(angle) * radius), mapY, mapHeight);
                }
                int candidateX = markerX;
                int candidateY = markerY;
                if (markers.stream().noneMatch(existing -> Math.abs(existing.x - candidateX) < MARKER_SIZE + 1
                        && Math.abs(existing.y - candidateY) < MARKER_SIZE + 1)) {
                    break;
                }
            }
            markers.add(new Marker(target, markerX, markerY));
        }
        this.cachedMapX = mapX;
        this.cachedMapY = mapY;
        this.cachedMarkers = List.copyOf(markers);
        return this.cachedMarkers;
    }

    private int[] mapPosition(int x, int z, int mapX, int mapY, int mapWidth, int mapHeight, boolean centerPoint) {
        int minX = Math.min(this.originX, this.targets.stream().mapToInt(OpenShadowTransferPayload.Target::x).min().orElse(this.originX));
        int maxX = Math.max(this.originX, this.targets.stream().mapToInt(OpenShadowTransferPayload.Target::x).max().orElse(this.originX));
        int minZ = Math.min(this.originZ, this.targets.stream().mapToInt(OpenShadowTransferPayload.Target::z).min().orElse(this.originZ));
        int maxZ = Math.max(this.originZ, this.targets.stream().mapToInt(OpenShadowTransferPayload.Target::z).max().orElse(this.originZ));
        int usableWidth = mapWidth - MAP_PADDING * 2 - MARKER_SIZE;
        int usableHeight = mapHeight - MAP_PADDING * 2 - MARKER_SIZE;
        double normalizedX = maxX == minX ? 0.5D : (x - minX) / (double) (maxX - minX);
        double normalizedZ = maxZ == minZ ? 0.5D : (z - minZ) / (double) (maxZ - minZ);
        int markerX = mapX + MAP_PADDING + (int) Math.round(normalizedX * usableWidth);
        int markerY = mapY + MAP_PADDING + (int) Math.round(normalizedZ * usableHeight);
        if (centerPoint) {
            markerX += MARKER_SIZE / 2;
            markerY += MARKER_SIZE / 2;
        }
        return new int[]{markerX, markerY};
    }

    private static void renderMapGrid(GuiGraphics gui, int x, int y, int width, int height) {
        int left = x + MAP_PADDING;
        int top = y + MAP_PADDING;
        int right = x + width - MAP_PADDING;
        int bottom = y + height - MAP_PADDING;
        for (int index = 1; index < 4; index++) {
            int gridX = left + (right - left) * index / 4;
            int gridY = top + (bottom - top) * index / 4;
            gui.fill(gridX, top, gridX + 1, bottom, 0x202D161A);
            gui.fill(left, gridY, right, gridY + 1, 0x202D161A);
        }
        gui.fill((left + right) / 2, top, (left + right) / 2 + 1, bottom, 0x304F2028);
        gui.fill(left, (top + bottom) / 2, right, (top + bottom) / 2 + 1, 0x304F2028);
    }

    private void selectTarget(int index) {
        if (index < 0 || index >= this.targets.size()) {
            return;
        }
        PacketDistributor.sendToServer(new SelectShadowTransferPayload(this.targets.get(index).familiarId()));
        this.onClose();
    }

    private static boolean inside(double mouseX, double mouseY, Marker marker) {
        return mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE
                && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE;
    }

    private static int clampMarkerX(int x, int mapX, int mapWidth) {
        return Math.clamp(x, mapX + MAP_PADDING, mapX + mapWidth - MAP_PADDING - MARKER_SIZE);
    }

    private static int clampMarkerY(int y, int mapY, int mapHeight) {
        return Math.clamp(y, mapY + MAP_PADDING, mapY + mapHeight - MAP_PADDING - MARKER_SIZE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Marker(OpenShadowTransferPayload.Target target, int x, int y) {
    }
}
