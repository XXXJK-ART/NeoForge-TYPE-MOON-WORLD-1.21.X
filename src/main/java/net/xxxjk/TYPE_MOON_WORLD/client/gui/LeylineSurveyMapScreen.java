package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LeylineSurveyMapScreen extends Screen {
    private static final ResourceLocation MAP_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/map/map_background.png");

    private static final int PANEL_WIDTH = 308;
    private static final int PANEL_HEIGHT = 186;
    private static final int MAP_BACKGROUND_SIZE = 142;
    private static final int MAP_CANVAS_SIZE = 128;
    private static final int MAP_CANVAS_OFFSET = 7;

    private final int gridSize;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final String dimensionId;
    private final byte[] concentrations;

    public LeylineSurveyMapScreen(int gridSize, int centerChunkX, int centerChunkZ, String dimensionId, byte[] concentrations) {
        super(Component.translatable("gui.typemoonworld.leyline_map.title"));
        this.gridSize = gridSize;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.concentrations = concentrations == null ? new byte[0] : concentrations.clone();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Keep world visible and avoid vanilla UI blur.
        guiGraphics.fill(0, 0, this.width, this.height, 0x55000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        renderPanel(guiGraphics, panelX, panelY);
        renderHeader(guiGraphics, panelX, panelY);
        renderMap(guiGraphics, panelX, panelY);
        renderLegend(guiGraphics, panelX, panelY);
        renderHoverTooltip(guiGraphics, panelX, panelY, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally left empty to disable vanilla blur/menu background.
    }

    private void renderPanel(GuiGraphics guiGraphics, int panelX, int panelY) {
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC101020);
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFF8FAFE0);
        guiGraphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF8FAFE0);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFF8FAFE0);
        guiGraphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF8FAFE0);
    }

    private void renderHeader(GuiGraphics guiGraphics, int panelX, int panelY) {
        guiGraphics.drawCenteredString(this.font, this.title, panelX + (PANEL_WIDTH / 2), panelY + 8, 0xEAF2FF);

        int textX = panelX + 164;
        int textY = panelY + 26;
        String dimShort = this.font.plainSubstrByWidth(this.dimensionId, 130);

        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.typemoonworld.leyline_map.dimension", dimShort),
                textX,
                textY,
                0xD9E6FF,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.typemoonworld.leyline_map.center", this.centerChunkX, this.centerChunkZ),
                textX,
                textY + 12,
                0xD9E6FF,
                false
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.typemoonworld.leyline_map.hint"),
                textX,
                textY + 24,
                0xA8B6D4,
                false
        );
    }

    private void renderMap(GuiGraphics guiGraphics, int panelX, int panelY) {
        int mapLeft = panelX + 12;
        int mapTop = panelY + 24;
        guiGraphics.blit(
                MAP_BACKGROUND,
                mapLeft,
                mapTop,
                0,
                0,
                MAP_BACKGROUND_SIZE,
                MAP_BACKGROUND_SIZE,
                MAP_BACKGROUND_SIZE,
                MAP_BACKGROUND_SIZE
        );

        int size = this.gridSize;
        if (size <= 0 || this.concentrations.length != size * size) {
            return;
        }

        int canvasLeft = mapLeft + MAP_CANVAS_OFFSET;
        int canvasTop = mapTop + MAP_CANVAS_OFFSET;
        Direction facing = getPlayerFacing();

        for (int z = 0; z < size; z++) {
            int y0 = canvasTop + ((z * MAP_CANVAS_SIZE) / size);
            int y1 = canvasTop + (((z + 1) * MAP_CANVAS_SIZE) / size);
            if (y1 <= y0) {
                y1 = y0 + 1;
            }

            for (int x = 0; x < size; x++) {
                int x0 = canvasLeft + ((x * MAP_CANVAS_SIZE) / size);
                int x1 = canvasLeft + (((x + 1) * MAP_CANVAS_SIZE) / size);
                if (x1 <= x0) {
                    x1 = x0 + 1;
                }

                Cell sample = mapDisplayCellToSampleCell(x, z, size, facing);
                int sampleX = sample.x;
                int sampleZ = sample.z;
                int index = (sampleZ * size) + sampleX;
                int concentration = this.concentrations[index] < 0 ? -1 : (this.concentrations[index] & 0xFF);
                guiGraphics.fill(x0, y0, x1, y1, colorForConcentration(concentration));
            }
        }

        Cell playerCell = mapSampleCellToDisplayCell(size / 2, size / 2, size, facing);
        int playerCellX = playerCell.x;
        int playerCellZ = playerCell.z;
        int cx0 = canvasLeft + ((playerCellX * MAP_CANVAS_SIZE) / size);
        int cx1 = canvasLeft + (((playerCellX + 1) * MAP_CANVAS_SIZE) / size);
        int cz0 = canvasTop + ((playerCellZ * MAP_CANVAS_SIZE) / size);
        int cz1 = canvasTop + (((playerCellZ + 1) * MAP_CANVAS_SIZE) / size);
        if (cx1 <= cx0) cx1 = cx0 + 1;
        if (cz1 <= cz0) cz1 = cz0 + 1;
        guiGraphics.fill(cx0, cz0, cx1, cz1, 0xCCFFFFFF);
    }

    private void renderLegend(GuiGraphics guiGraphics, int panelX, int panelY) {
        int legendX = panelX + 164;
        int legendY = panelY + 74;

        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.typemoonworld.leyline_map.legend_title"),
                legendX,
                legendY,
                0xEAF2FF,
                false
        );

        drawLegendRow(guiGraphics, legendX, legendY + 14, colorForConcentration(15), Component.translatable("gui.typemoonworld.leyline_map.legend.low"));
        drawLegendRow(guiGraphics, legendX, legendY + 28, colorForConcentration(40), Component.translatable("gui.typemoonworld.leyline_map.legend.mid"));
        drawLegendRow(guiGraphics, legendX, legendY + 42, colorForConcentration(65), Component.translatable("gui.typemoonworld.leyline_map.legend.high"));
        drawLegendRow(guiGraphics, legendX, legendY + 56, colorForConcentration(90), Component.translatable("gui.typemoonworld.leyline_map.legend.extreme"));
        drawLegendRow(guiGraphics, legendX, legendY + 70, colorForConcentration(-1), Component.translatable("gui.typemoonworld.leyline_map.legend.unloaded"));
    }

    private void drawLegendRow(GuiGraphics guiGraphics, int x, int y, int color, Component text) {
        guiGraphics.fill(x, y + 2, x + 10, y + 10, color);
        guiGraphics.drawString(this.font, text, x + 14, y + 1, 0xD9E6FF, false);
    }

    private void renderHoverTooltip(GuiGraphics guiGraphics, int panelX, int panelY, int mouseX, int mouseY) {
        int size = this.gridSize;
        if (size <= 0 || this.concentrations.length != size * size) {
            return;
        }

        int mapLeft = panelX + 12;
        int mapTop = panelY + 24;
        int canvasLeft = mapLeft + MAP_CANVAS_OFFSET;
        int canvasTop = mapTop + MAP_CANVAS_OFFSET;
        int canvasRight = canvasLeft + MAP_CANVAS_SIZE;
        int canvasBottom = canvasTop + MAP_CANVAS_SIZE;

        if (mouseX < canvasLeft || mouseX >= canvasRight || mouseY < canvasTop || mouseY >= canvasBottom) {
            return;
        }

        int cellX = ((mouseX - canvasLeft) * size) / MAP_CANVAS_SIZE;
        int cellZ = ((mouseY - canvasTop) * size) / MAP_CANVAS_SIZE;
        cellX = Mth.clamp(cellX, 0, size - 1);
        cellZ = Mth.clamp(cellZ, 0, size - 1);

        Cell sample = mapDisplayCellToSampleCell(cellX, cellZ, size, getPlayerFacing());
        int sampleX = sample.x;
        int sampleZ = sample.z;
        int startChunkX = this.centerChunkX - (size / 2);
        int startChunkZ = this.centerChunkZ - (size / 2);
        int chunkX = startChunkX + sampleX;
        int chunkZ = startChunkZ + sampleZ;
        int index = (sampleZ * size) + sampleX;
        int concentration = this.concentrations[index] < 0 ? -1 : (this.concentrations[index] & 0xFF);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.typemoonworld.leyline_map.tooltip.chunk", chunkX, chunkZ));
        if (concentration < 0) {
            lines.add(Component.translatable("gui.typemoonworld.leyline_map.tooltip.unloaded"));
        } else {
            lines.add(Component.translatable("gui.typemoonworld.leyline_map.tooltip.concentration", concentration));
        }
        guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
    }

    private static int colorForConcentration(int concentration) {
        if (concentration < 0) {
            return 0xFF30343F;
        }
        if (concentration < 30) {
            float t = concentration / 29.0F;
            return lerpColor(0xFF1B2A49, 0xFF3B82F6, t);
        }
        if (concentration < 50) {
            float t = (concentration - 30) / 19.0F;
            return lerpColor(0xFF14532D, 0xFF22C55E, t);
        }
        if (concentration < 80) {
            float t = (concentration - 50) / 29.0F;
            return lerpColor(0xFFB45309, 0xFFF59E0B, t);
        }
        float t = (concentration - 80) / 20.0F;
        return lerpColor(0xFFB91C1C, 0xFFF87171, t);
    }

    private static int lerpColor(int from, int to, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a0 = (from >>> 24) & 0xFF;
        int r0 = (from >>> 16) & 0xFF;
        int g0 = (from >>> 8) & 0xFF;
        int b0 = from & 0xFF;

        int a1 = (to >>> 24) & 0xFF;
        int r1 = (to >>> 16) & 0xFF;
        int g1 = (to >>> 8) & 0xFF;
        int b1 = to & 0xFF;

        int a = Mth.floor(Mth.lerp(clamped, a0, a1));
        int r = Mth.floor(Mth.lerp(clamped, r0, r1));
        int g = Mth.floor(Mth.lerp(clamped, g0, g1));
        int b = Mth.floor(Mth.lerp(clamped, b0, b1));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static Direction getPlayerFacing() {
        if (Minecraft.getInstance().player == null) {
            return Direction.NORTH;
        }
        return Minecraft.getInstance().player.getDirection();
    }

    private static Cell mapDisplayCellToSampleCell(int displayX, int displayZ, int size, Direction facing) {
        int center = size / 2;
        int dx = displayX - center;
        int dz = displayZ - center;

        int sampleDx;
        int sampleDz;
        switch (facing) {
            case SOUTH -> {
                sampleDx = -dx;
                sampleDz = -dz;
            }
            case EAST -> {
                sampleDx = -dz;
                sampleDz = dx;
            }
            case WEST -> {
                sampleDx = dz;
                sampleDz = -dx;
            }
            case NORTH, UP, DOWN -> {
                sampleDx = dx;
                sampleDz = dz;
            }
            default -> {
                sampleDx = dx;
                sampleDz = dz;
            }
        }

        int sampleX = Mth.clamp(center + sampleDx, 0, size - 1);
        int sampleZ = Mth.clamp(center + sampleDz, 0, size - 1);
        return new Cell(sampleX, sampleZ);
    }

    private static Cell mapSampleCellToDisplayCell(int sampleX, int sampleZ, int size, Direction facing) {
        int center = size / 2;
        int sampleDx = sampleX - center;
        int sampleDz = sampleZ - center;

        int dx;
        int dz;
        switch (facing) {
            case SOUTH -> {
                dx = -sampleDx;
                dz = -sampleDz;
            }
            case EAST -> {
                dx = sampleDz;
                dz = -sampleDx;
            }
            case WEST -> {
                dx = -sampleDz;
                dz = sampleDx;
            }
            case NORTH, UP, DOWN -> {
                dx = sampleDx;
                dz = sampleDz;
            }
            default -> {
                dx = sampleDx;
                dz = sampleDz;
            }
        }

        int displayX = Mth.clamp(center + dx, 0, size - 1);
        int displayZ = Mth.clamp(center + dz, 0, size - 1);
        return new Cell(displayX, displayZ);
    }

    private record Cell(int x, int z) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
