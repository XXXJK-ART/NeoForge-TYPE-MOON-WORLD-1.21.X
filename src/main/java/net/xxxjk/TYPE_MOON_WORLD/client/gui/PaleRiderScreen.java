package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderSelectMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderSpawnModeMessage;

public final class PaleRiderScreen extends Screen {
   private static final int PALE_ACCENT = 0xFFB9C7CC;
   private static final int PANEL_WIDTH = 244;
   private static final int MAP_PANEL_WIDTH = 308;
   private static final int MAP_PANEL_HEIGHT = 196;
   private static final int MAP_PADDING = 12;
   private static final int MARKER_SIZE = 14;

   private final int kind;
   private final List<PaleRiderOpenScreenMessage.Target> targets;
   private PaleRiderOpenScreenMessage.Target hoveredTarget;
   private int selectedTargetIndex;
   private List<Marker> cachedMarkers = List.of();
   private int cachedMapX = Integer.MIN_VALUE;
   private int cachedMapY = Integer.MIN_VALUE;
   private int cachedMapWidth = Integer.MIN_VALUE;
   private int cachedMapHeight = Integer.MIN_VALUE;

   public PaleRiderScreen(int kind, List<PaleRiderOpenScreenMessage.Target> targets) {
      super(Component.translatable(kind == 2 ? "screen.typemoonworld.pale_rider.possession"
         : kind == 3 ? "screen.typemoonworld.pale_rider.command" : "screen.typemoonworld.pale_rider.spawn"));
      this.kind = kind;
      this.targets = targets == null ? List.of() : List.copyOf(targets);
      this.selectedTargetIndex = this.targets.isEmpty() ? -1 : 0;
   }

   @Override
   protected void init() {
      if (this.kind == 2) return;
      int columns = 2;
      int buttonWidth = 104;
      int buttonHeight = 22;
      int gap = 8;
      int startX = (this.width - (buttonWidth * columns + gap)) / 2;
      int startY = this.height / 2 - (this.choiceCount() > 2 ? 22 : 11);

      if (this.kind == 0 || this.kind == 1) {
         int[] modes = this.kind == 1 ? new int[]{2, 3} : new int[]{0, 1, 2, 3};
         String[] keys = this.kind == 1 ? new String[]{"single_rat", "single_crow"}
            : new String[]{"rat_swarm", "crow", "single_rat", "single_crow"};
         for (int i = 0; i < modes.length; i++) {
            int mode = modes[i];
            int x = startX + i % columns * (buttonWidth + gap);
            int y = startY + i / columns * (buttonHeight + gap);
            this.addRenderableWidget(new NeonButton(x, y, buttonWidth, buttonHeight,
               Component.translatable("screen.typemoonworld.pale_rider." + keys[i]), button -> this.selectSpawn(mode),
               i % 2 == 0 ? GuiUtils.ARCANE_CREST : PALE_ACCENT).setArcaneStyle(true));
         }
         return;
      }

      String[] labels = {"free", "hold", "attack", "gather", "lethal"};
      int[] colors = {GuiUtils.ARCANE_VALID, GuiUtils.ARCANE_GOLD, GuiUtils.ARCANE_DANGER, PALE_ACCENT, 0xFFD64B5A};
      for (int i = 0; i < labels.length; i++) {
         int command = i;
         int x = startX + i % columns * (buttonWidth + gap);
         int y = startY + i / columns * (buttonHeight + gap);
         this.addRenderableWidget(new NeonButton(x, y, buttonWidth, buttonHeight,
            Component.translatable("screen.typemoonworld.pale_rider.command." + labels[i]), button -> this.selectCommand(command), colors[i])
            .setArcaneStyle(true));
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
      if (this.kind == 2) this.renderPossessionMap(gui, mouseX, mouseY);
      else this.renderChoicePanel(gui);
      super.render(gui, mouseX, mouseY, partialTick);
      if (this.hoveredTarget != null) {
         gui.renderTooltip(this.font, List.of(
            Component.literal(this.hoveredTarget.name()),
            Component.translatable("screen.typemoonworld.pale_rider.coordinates", this.hoveredTarget.x(), this.hoveredTarget.z())
         ), java.util.Optional.empty(), mouseX, mouseY);
      }
   }

   @Override
   public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      // Keep the world sharp behind Pale Rider's tactical screens.
   }

   private void renderChoicePanel(GuiGraphics gui) {
      int panelHeight = this.choiceCount() > 4 ? 142 : this.choiceCount() > 2 ? 112 : 82;
      int panelX = (this.width - PANEL_WIDTH) / 2;
      int panelY = (this.height - panelHeight) / 2 - 8;
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, PANEL_WIDTH, panelHeight, PALE_ACCENT);
      gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
   }

   private void renderPossessionMap(GuiGraphics gui, int mouseX, int mouseY) {
      int panelX = (this.width - MAP_PANEL_WIDTH) / 2;
      int panelY = (this.height - MAP_PANEL_HEIGHT) / 2;
      int mapX = panelX + 12;
      int mapY = panelY + 35;
      int mapWidth = MAP_PANEL_WIDTH - 24;
      int mapHeight = MAP_PANEL_HEIGHT - 47;
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, MAP_PANEL_WIDTH, MAP_PANEL_HEIGHT, PALE_ACCENT);
      GuiUtils.renderArcanePanel(gui, mapX, mapY, mapWidth, mapHeight, PALE_ACCENT);
      gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
      gui.drawString(this.font, Component.translatable("screen.typemoonworld.pale_rider.target_count", this.targets.size()),
         panelX + MAP_PANEL_WIDTH - 12 - this.font.width(Component.translatable("screen.typemoonworld.pale_rider.target_count", this.targets.size())),
         panelY + 9, GuiUtils.ARCANE_TEXT_MUTED, false);

      this.renderMapGrid(gui, mapX, mapY, mapWidth, mapHeight);
      this.hoveredTarget = null;
      if (this.targets.isEmpty()) {
         gui.drawCenteredString(this.font, Component.translatable("screen.typemoonworld.pale_rider.no_targets"),
            mapX + mapWidth / 2, mapY + mapHeight / 2 - 4, GuiUtils.ARCANE_TEXT_MUTED);
         return;
      }

      List<Marker> markers = this.layoutMarkers(mapX, mapY, mapWidth, mapHeight);
      for (int i = 0; i < markers.size(); i++) {
         Marker marker = markers.get(i);
         boolean hovered = mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE
            && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE;
         boolean selected = i == this.selectedTargetIndex;
         int accent = hovered ? GuiUtils.ARCANE_VALID : selected ? GuiUtils.ARCANE_GOLD : PALE_ACCENT;
         int fill = hovered || selected ? 0xF02A3840 : GuiUtils.ARCANE_PANEL_ALT;
         gui.fill(marker.x, marker.y, marker.x + MARKER_SIZE, marker.y + MARKER_SIZE, fill);
         gui.renderOutline(marker.x, marker.y, MARKER_SIZE, MARKER_SIZE, accent);
         gui.fill(marker.x + 1, marker.y + 1, marker.x + 3, marker.y + MARKER_SIZE - 1, accent);
         gui.fill(marker.x + 5, marker.y + 5, marker.x + 9, marker.y + 9, accent);
         if (hovered) {
            this.selectedTargetIndex = i;
            this.hoveredTarget = marker.target;
         }
      }
   }

   private void renderMapGrid(GuiGraphics gui, int x, int y, int width, int height) {
      int left = x + MAP_PADDING;
      int top = y + MAP_PADDING;
      int right = x + width - MAP_PADDING;
      int bottom = y + height - MAP_PADDING;
      for (int i = 1; i < 4; i++) {
         int gridX = left + (right - left) * i / 4;
         int gridY = top + (bottom - top) * i / 4;
         gui.fill(gridX, top, gridX + 1, bottom, 0x2034404C);
         gui.fill(left, gridY, right, gridY + 1, 0x2034404C);
      }
      gui.fill((left + right) / 2, top, (left + right) / 2 + 1, bottom, 0x304D5A64);
      gui.fill(left, (top + bottom) / 2, right, (top + bottom) / 2 + 1, 0x304D5A64);
   }

   private List<Marker> layoutMarkers(int mapX, int mapY, int mapWidth, int mapHeight) {
      if (mapX == this.cachedMapX && mapY == this.cachedMapY
         && mapWidth == this.cachedMapWidth && mapHeight == this.cachedMapHeight) {
         return this.cachedMarkers;
      }
      int minX = this.targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::x).min().orElse(0);
      int maxX = this.targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::x).max().orElse(minX);
      int minZ = this.targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::z).min().orElse(0);
      int maxZ = this.targets.stream().mapToInt(PaleRiderOpenScreenMessage.Target::z).max().orElse(minZ);
      int usableWidth = mapWidth - MAP_PADDING * 2 - MARKER_SIZE;
      int usableHeight = mapHeight - MAP_PADDING * 2 - MARKER_SIZE;
      int areaLeft = mapX + MAP_PADDING;
      int areaTop = mapY + MAP_PADDING;
      int areaRight = areaLeft + usableWidth;
      int areaBottom = areaTop + usableHeight;
      List<Marker> markers = new ArrayList<>(this.targets.size());
      for (PaleRiderOpenScreenMessage.Target target : this.targets) {
         double normalizedX = maxX == minX ? 0.5 : (target.x() - minX) / (double)(maxX - minX);
         double normalizedZ = maxZ == minZ ? 0.5 : (target.z() - minZ) / (double)(maxZ - minZ);
         int baseX = areaLeft + (int)Math.round(normalizedX * usableWidth);
         int baseY = areaTop + (int)Math.round(normalizedZ * usableHeight);
         int markerX = baseX;
         int markerY = baseY;
         for (int attempt = 0; attempt < 192; attempt++) {
            if (attempt > 0) {
               double radius = 4.0 + Math.sqrt(attempt) * 7.0;
               double angle = attempt * 2.399963229728653;
               markerX = Math.max(areaLeft, Math.min(areaRight, baseX + (int)Math.round(Math.cos(angle) * radius)));
               markerY = Math.max(areaTop, Math.min(areaBottom, baseY + (int)Math.round(Math.sin(angle) * radius)));
            }
            int candidateX = markerX;
            int candidateY = markerY;
            if (markers.stream().noneMatch(existing -> Math.abs(existing.x - candidateX) < MARKER_SIZE + 1
               && Math.abs(existing.y - candidateY) < MARKER_SIZE + 1)) break;
         }
         markers.add(new Marker(target, markerX, markerY));
      }
      this.cachedMapX = mapX;
      this.cachedMapY = mapY;
      this.cachedMapWidth = mapWidth;
      this.cachedMapHeight = mapHeight;
      this.cachedMarkers = List.copyOf(markers);
      return this.cachedMarkers;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.kind == 2 && button == 0) {
         int panelX = (this.width - MAP_PANEL_WIDTH) / 2;
         int panelY = (this.height - MAP_PANEL_HEIGHT) / 2;
         int mapX = panelX + 12;
         int mapY = panelY + 35;
         int mapWidth = MAP_PANEL_WIDTH - 24;
         int mapHeight = MAP_PANEL_HEIGHT - 47;
         List<Marker> markers = this.layoutMarkers(mapX, mapY, mapWidth, mapHeight);
         for (int i = markers.size() - 1; i >= 0; i--) {
            Marker marker = markers.get(i);
            if (mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE) {
               this.selectedTargetIndex = i;
               this.selectEntity(marker.target.entityId());
               return true;
            }
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.kind == 2 && !this.targets.isEmpty() && scrollY != 0.0) {
         int direction = scrollY > 0.0 ? -1 : 1;
         this.selectedTargetIndex = Math.floorMod(this.selectedTargetIndex + direction, this.targets.size());
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.kind == 2 && !this.targets.isEmpty()) {
         if (keyCode == 262 || keyCode == 264) {
            this.selectedTargetIndex = (this.selectedTargetIndex + 1) % this.targets.size();
            return true;
         }
         if (keyCode == 263 || keyCode == 265) {
            this.selectedTargetIndex = Math.floorMod(this.selectedTargetIndex - 1, this.targets.size());
            return true;
         }
         if (keyCode == 257 || keyCode == 335) {
            this.selectEntity(this.targets.get(this.selectedTargetIndex).entityId());
            return true;
         }
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private int choiceCount() {
      return this.kind == 1 ? 2 : this.kind == 3 ? 5 : 4;
   }

   private void selectSpawn(int mode) {
      PacketDistributor.sendToServer(new PaleRiderSpawnModeMessage(mode), new CustomPacketPayload[0]);
      this.onClose();
   }

   private void selectEntity(int id) {
      PacketDistributor.sendToServer(new PaleRiderSelectMessage(id), new CustomPacketPayload[0]);
      this.onClose();
   }

   private void selectCommand(int command) {
      PacketDistributor.sendToServer(new PaleRiderCommandMessage(command), new CustomPacketPayload[0]);
      this.onClose();
   }

   private record Marker(PaleRiderOpenScreenMessage.Target target, int x, int y) {}
}
