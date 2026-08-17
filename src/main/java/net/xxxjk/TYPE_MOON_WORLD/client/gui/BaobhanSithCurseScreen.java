package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseRequestMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseTriggerMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.baobhan.BaobhanSithCurseService;

public final class BaobhanSithCurseScreen extends Screen {
   private static final int ACCENT = 0xFFB31335;
   private static final int PURPLE = 0xFF742095;
   private static final int PANEL_WIDTH = 446;
   private static final int PANEL_HEIGHT = 270;
   private static final int MAP_WIDTH = 268;
   private static final int MAP_HEIGHT = 188;
   private static final int MAP_PADDING = 14;
   private static final int MARKER_SIZE = 14;
   private static final int MEDIUM_BUTTON_WIDTH = 66;
   private static final int MEDIUM_BUTTON_HEIGHT = 19;
   private static final int NP_BUTTON_HEIGHT = 22;
   private static final UUID ALL_TARGETS_UUID = new UUID(0L, 0L);

   private List<BaobhanSithCurseOpenScreenMessage.Target> targets;
   private UUID selected;
   private int refreshTicks;
   private BaobhanSithCurseOpenScreenMessage.Target hoveredTarget;
   private boolean noblePhantasmMode;
   private List<Marker> cachedMarkers = List.of();
   private int cachedMapX = Integer.MIN_VALUE;
   private int cachedMapY = Integer.MIN_VALUE;
   private int cachedMapWidth = Integer.MIN_VALUE;
   private int cachedMapHeight = Integer.MIN_VALUE;
   private int cachedPlayerX = Integer.MIN_VALUE;
   private int cachedPlayerZ = Integer.MIN_VALUE;
   private String cachedDimension = "";
   private final List<MediumButton> mediumButtons = new ArrayList<>();
   private final List<NoblePhantasmButton> noblePhantasmButtons = new ArrayList<>();

   public BaobhanSithCurseScreen(List<BaobhanSithCurseOpenScreenMessage.Target> targets, boolean noblePhantasmMode) {
      super(Component.translatable(noblePhantasmMode
         ? "screen.typemoonworld.baobhan_sith.fetch_failnaught_map"
         : "screen.typemoonworld.baobhan_sith.curse_panel"));
      this.noblePhantasmMode = noblePhantasmMode;
      this.targets = targets == null ? List.of() : List.copyOf(targets);
      if (!this.noblePhantasmMode && !this.targets.isEmpty()) {
         this.selected = this.targets.get(0).uuid();
      }
   }

   public void updateTargets(List<BaobhanSithCurseOpenScreenMessage.Target> targets, boolean noblePhantasmMode) {
      boolean modeChanged = this.noblePhantasmMode != noblePhantasmMode;
      this.noblePhantasmMode = noblePhantasmMode;
      this.targets = targets == null ? List.of() : List.copyOf(targets);
      if (modeChanged && this.noblePhantasmMode) {
         this.selected = null;
      }
      if (this.selected != null && this.targets.stream().noneMatch(target -> target.uuid().equals(this.selected))) {
         this.selected = this.targets.isEmpty() || this.noblePhantasmMode ? null : this.targets.get(0).uuid();
      }
      if (!this.noblePhantasmMode && this.selected == null && !this.targets.isEmpty()) {
         this.selected = this.targets.get(0).uuid();
      }
      this.invalidateMarkerCache();
   }

   @Override
   public void tick() {
      if (++this.refreshTicks >= 20) {
         this.refreshTicks = 0;
         PacketDistributor.sendToServer(new BaobhanSithCurseRequestMessage(this.noblePhantasmMode), new CustomPacketPayload[0]);
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
      int panelX = (this.width - PANEL_WIDTH) / 2;
      int panelY = (this.height - PANEL_HEIGHT) / 2;
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, ACCENT);
      Component currentTitle = Component.translatable(this.noblePhantasmMode
         ? "screen.typemoonworld.baobhan_sith.fetch_failnaught_map"
         : "screen.typemoonworld.baobhan_sith.curse_panel");
      gui.drawCenteredString(this.font, currentTitle, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
      Component count = Component.translatable("screen.typemoonworld.baobhan_sith.target_count", this.targets.size());
      gui.drawString(this.font, count, panelX + PANEL_WIDTH - 12 - this.font.width(count), panelY + 9, GuiUtils.ARCANE_TEXT_MUTED, false);

      LocalPlayer player = Minecraft.getInstance().player;
      int playerX = player == null ? 0 : player.blockPosition().getX();
      int playerY = player == null ? 0 : player.blockPosition().getY();
      int playerZ = player == null ? 0 : player.blockPosition().getZ();
      String dimension = player == null ? "" : player.level().dimension().location().toString();

      int mapX = panelX + 14;
      int mapY = panelY + 43;
      GuiUtils.renderArcanePanel(gui, mapX, mapY, MAP_WIDTH, MAP_HEIGHT, ACCENT);
      Component self = Component.translatable("screen.typemoonworld.baobhan_sith.self_position", playerX, playerY, playerZ);
      gui.drawString(this.font, self, mapX, panelY + 28, GuiUtils.ARCANE_TEXT_MUTED, false);
      this.renderMapGrid(gui, mapX, mapY, MAP_WIDTH, MAP_HEIGHT);
      this.renderPlayerMarker(gui, mapX, mapY, MAP_WIDTH, MAP_HEIGHT);

      this.hoveredTarget = null;
      this.mediumButtons.clear();
      this.noblePhantasmButtons.clear();
      if (this.targets.isEmpty()) {
         gui.drawCenteredString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.no_targets"),
            mapX + MAP_WIDTH / 2, mapY + MAP_HEIGHT / 2 - 4, GuiUtils.ARCANE_TEXT_MUTED);
      } else {
         this.renderTargetMarkers(gui, mouseX, mouseY, mapX, mapY, MAP_WIDTH, MAP_HEIGHT, playerX, playerZ, dimension);
      }

      int detailX = mapX + MAP_WIDTH + 12;
      int detailY = mapY;
      int detailW = PANEL_WIDTH - (detailX - panelX) - 14;
      this.renderDetails(gui, detailX, detailY, detailW, MAP_HEIGHT, mouseX, mouseY, playerX, playerY, playerZ, dimension);

      super.render(gui, mouseX, mouseY, partialTick);
      if (this.hoveredTarget != null) {
         gui.renderTooltip(this.font, tooltipFor(this.hoveredTarget, playerX, playerY, playerZ, dimension),
            java.util.Optional.empty(), mouseX, mouseY);
      }
   }

   @Override
   public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.noblePhantasmMode) {
         for (NoblePhantasmButton npButton : this.noblePhantasmButtons) {
            if (npButton.contains(mouseX, mouseY)) {
               if (npButton.allTargets) {
                  this.detonateAllTargets();
               } else if (npButton.target != null) {
                  this.detonateTarget(npButton.target);
               }
               return true;
            }
         }
      }
      for (MediumButton mediumButton : this.mediumButtons) {
         if (mediumButton.contains(mouseX, mouseY)) {
            if (this.noblePhantasmMode) {
               this.detonateTarget(mediumButton.target);
            } else {
               PacketDistributor.sendToServer(new BaobhanSithCurseTriggerMessage(mediumButton.target.uuid(), mediumButton.medium, false, false, false),
                  new CustomPacketPayload[0]);
            }
            return true;
         }
      }
      Marker marker = markerAt(mouseX, mouseY);
      if (marker != null) {
         this.selectTarget(marker.target);
         return true;
      }
      if (this.noblePhantasmMode && this.selected != null) {
         this.clearSelectedTarget();
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0.0 && !this.targets.isEmpty()) {
         int current = selectedIndex();
         int direction = scrollY > 0.0 ? -1 : 1;
         BaobhanSithCurseOpenScreenMessage.Target target = this.targets.get(Math.floorMod(current + direction, this.targets.size()));
         this.selectTarget(target);
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.targets.isEmpty()) {
         if (keyCode == 262 || keyCode == 264) {
            BaobhanSithCurseOpenScreenMessage.Target target = this.targets.get((selectedIndex() + 1) % this.targets.size());
            this.selectTarget(target);
            return true;
         }
         if (keyCode == 263 || keyCode == 265) {
            BaobhanSithCurseOpenScreenMessage.Target target = this.targets.get(Math.floorMod(selectedIndex() - 1, this.targets.size()));
            this.selectTarget(target);
            return true;
         }
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void renderMapGrid(GuiGraphics gui, int x, int y, int width, int height) {
      int left = x + MAP_PADDING;
      int top = y + MAP_PADDING;
      int right = x + width - MAP_PADDING;
      int bottom = y + height - MAP_PADDING;
      for (int i = 1; i < 4; i++) {
         int gridX = left + (right - left) * i / 4;
         int gridY = top + (bottom - top) * i / 4;
         gui.fill(gridX, top, gridX + 1, bottom, 0x20380D18);
         gui.fill(left, gridY, right, gridY + 1, 0x20380D18);
      }
      gui.fill((left + right) / 2, top, (left + right) / 2 + 1, bottom, 0x40B31335);
      gui.fill(left, (top + bottom) / 2, right, (top + bottom) / 2 + 1, 0x40B31335);
   }

   private void renderPlayerMarker(GuiGraphics gui, int mapX, int mapY, int mapWidth, int mapHeight) {
      int centerX = mapX + mapWidth / 2;
      int centerY = mapY + mapHeight / 2;
      gui.fill(centerX - 3, centerY - 3, centerX + 4, centerY + 4, 0xF0E9D4D9);
      gui.renderOutline(centerX - 5, centerY - 5, 10, 10, 0xFFE9D4D9);
   }

   private void renderTargetMarkers(GuiGraphics gui, int mouseX, int mouseY, int mapX, int mapY, int mapWidth, int mapHeight,
                                    int playerX, int playerZ, String dimension) {
      for (Marker marker : layoutMarkers(mapX, mapY, mapWidth, mapHeight, playerX, playerZ, dimension)) {
         boolean hovered = mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE
            && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE;
         boolean selectedMarker = marker.target.uuid().equals(this.selected);
         boolean sameDimension = marker.target.dimension().equals(dimension);
         int accent = hovered ? 0xFFFF6F8D : selectedMarker ? ACCENT : sameDimension ? PURPLE : 0xFF8E8A92;
         int fill = hovered || selectedMarker ? 0xF03D101E : 0xD0180710;
         gui.fill(marker.x, marker.y, marker.x + MARKER_SIZE, marker.y + MARKER_SIZE, fill);
         gui.renderOutline(marker.x, marker.y, MARKER_SIZE, MARKER_SIZE, accent);
         gui.fill(marker.x + 6, marker.y + 3, marker.x + 9, marker.y + MARKER_SIZE - 3, accent);
         gui.fill(marker.x + 3, marker.y + 6, marker.x + MARKER_SIZE - 3, marker.y + 9, accent);
         if (hovered) {
            this.hoveredTarget = marker.target;
         }
      }
   }

   private void renderDetails(GuiGraphics gui, int x, int y, int width, int height, int mouseX, int mouseY,
                              int playerX, int playerY, int playerZ, String dimension) {
      GuiUtils.renderArcanePanel(gui, x, y, width, height, PURPLE);
      BaobhanSithCurseOpenScreenMessage.Target target = selectedTarget();
      if (target == null) {
         gui.drawCenteredString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.no_selected"),
            x + width / 2, y + height / 2 - 4, GuiUtils.ARCANE_TEXT_MUTED);
         if (this.noblePhantasmMode) {
            this.renderNoblePhantasmButton(gui, null, true, x + 8, y + height - NP_BUTTON_HEIGHT - 10,
               width - 16, mouseX, mouseY);
         }
         return;
      }
      gui.drawString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.selected"), x + 8, y + 8, GuiUtils.ARCANE_TEXT_MUTED, false);
      gui.drawString(this.font, Component.literal(trimToWidth(target.name(), width - 16)), x + 8, y + 20, GuiUtils.ARCANE_TEXT, false);
      gui.drawString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.target_info",
         target.x(), target.y(), target.z(), Math.round(target.health()), Math.round(target.maxHealth()), target.layers()),
         x + 8, y + 35, GuiUtils.ARCANE_TEXT_MUTED, false);
      gui.drawString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.dimension", shortDimension(target.dimension())),
         x + 8, y + 48, target.dimension().equals(dimension) ? 0xFFE9A0B3 : 0xFFC9C0CC, false);
      Component distance = target.dimension().equals(dimension)
         ? Component.translatable("screen.typemoonworld.baobhan_sith.distance", Math.round(distanceTo(target, playerX, playerY, playerZ)))
         : Component.translatable("screen.typemoonworld.baobhan_sith.distance_other_dimension");
      gui.drawString(this.font, distance, x + 8, y + 61, GuiUtils.ARCANE_TEXT_MUTED, false);
      gui.drawString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.medium_summary",
         target.blood(), target.skin(), target.hair(), target.remains()), x + 8, y + 79, 0xFFE9A0B3, false);
      gui.drawString(this.font, Component.translatable("screen.typemoonworld.baobhan_sith.curse_summary",
         target.bloodCurse(), target.skinCurse(), target.hairCurse(), target.remainsCurse()), x + 8, y + 92, GuiUtils.ARCANE_TEXT_MUTED, false);

      if (this.noblePhantasmMode) {
         int buttonX = x + 8;
         int buttonW = width - 16;
         this.renderNoblePhantasmButton(gui, target, false, buttonX, y + 120, buttonW, mouseX, mouseY);
         this.renderNoblePhantasmButton(gui, null, true, buttonX, y + 150, buttonW, mouseX, mouseY);
         return;
      }

      int buttonX = x + 8;
      int buttonY = y + 116;
      this.renderMediumButton(gui, target, BaobhanSithCurseService.MEDIUM_BLOOD, target.blood(), buttonX, buttonY, mouseX, mouseY);
      this.renderMediumButton(gui, target, BaobhanSithCurseService.MEDIUM_SKIN, target.skin(), buttonX + MEDIUM_BUTTON_WIDTH + 6, buttonY, mouseX, mouseY);
      this.renderMediumButton(gui, target, BaobhanSithCurseService.MEDIUM_HAIR, target.hair(), buttonX, buttonY + MEDIUM_BUTTON_HEIGHT + 7, mouseX, mouseY);
      this.renderMediumButton(gui, target, BaobhanSithCurseService.MEDIUM_REMAINS, target.remains(), buttonX + MEDIUM_BUTTON_WIDTH + 6, buttonY + MEDIUM_BUTTON_HEIGHT + 7, mouseX, mouseY);
   }

   private void renderNoblePhantasmButton(GuiGraphics gui, BaobhanSithCurseOpenScreenMessage.Target target, boolean allTargets,
                                          int x, int y, int width, int mouseX, int mouseY) {
      boolean enabled = allTargets ? this.targets.stream().anyMatch(this::hasPayload) : target != null && hasPayload(target);
      boolean hovered = enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + NP_BUTTON_HEIGHT;
      int accent = enabled ? (hovered ? 0xFFFF5475 : ACCENT) : 0xFF4B3B42;
      gui.fill(x, y, x + width, y + NP_BUTTON_HEIGHT, enabled ? 0xD0260914 : 0x90201B1F);
      gui.renderOutline(x, y, width, NP_BUTTON_HEIGHT, accent);
      Component label = Component.translatable(allTargets
         ? "screen.typemoonworld.baobhan_sith.fetch_failnaught_all_button"
         : "screen.typemoonworld.baobhan_sith.fetch_failnaught_target_button");
      gui.drawCenteredString(this.font, label, x + width / 2, y + 7, enabled ? GuiUtils.ARCANE_TEXT : GuiUtils.ARCANE_TEXT_MUTED);
      if (enabled) {
         this.noblePhantasmButtons.add(new NoblePhantasmButton(target, allTargets, x, y, width, NP_BUTTON_HEIGHT));
      }
   }

   private void renderMediumButton(GuiGraphics gui, BaobhanSithCurseOpenScreenMessage.Target target, String medium, int count, int x, int y, int mouseX, int mouseY) {
      boolean enabled = count > 0;
      boolean hovered = enabled && mouseX >= x && mouseX < x + MEDIUM_BUTTON_WIDTH && mouseY >= y && mouseY < y + MEDIUM_BUTTON_HEIGHT;
      int accent = enabled ? (hovered ? 0xFFFF5475 : ACCENT) : 0xFF4B3B42;
      gui.fill(x, y, x + MEDIUM_BUTTON_WIDTH, y + MEDIUM_BUTTON_HEIGHT, enabled ? 0xD0260914 : 0x90201B1F);
      gui.renderOutline(x, y, MEDIUM_BUTTON_WIDTH, MEDIUM_BUTTON_HEIGHT, accent);
      Component label = Component.translatable("screen.typemoonworld.baobhan_sith.medium_button",
         Component.translatable("screen.typemoonworld.baobhan_sith.medium." + medium.toLowerCase(java.util.Locale.ROOT)), count);
      gui.drawCenteredString(this.font, label, x + MEDIUM_BUTTON_WIDTH / 2, y + 5, enabled ? GuiUtils.ARCANE_TEXT : GuiUtils.ARCANE_TEXT_MUTED);
      if (enabled) {
         this.mediumButtons.add(new MediumButton(target, medium, x, y));
      }
   }

   private List<Marker> layoutMarkers(int mapX, int mapY, int mapWidth, int mapHeight, int playerX, int playerZ, String dimension) {
      if (mapX == this.cachedMapX && mapY == this.cachedMapY && mapWidth == this.cachedMapWidth && mapHeight == this.cachedMapHeight
         && playerX == this.cachedPlayerX && playerZ == this.cachedPlayerZ && dimension.equals(this.cachedDimension)) {
         return this.cachedMarkers;
      }
      int usableWidth = mapWidth - MAP_PADDING * 2 - MARKER_SIZE;
      int usableHeight = mapHeight - MAP_PADDING * 2 - MARKER_SIZE;
      int areaLeft = mapX + MAP_PADDING;
      int areaTop = mapY + MAP_PADDING;
      int areaRight = areaLeft + usableWidth;
      int areaBottom = areaTop + usableHeight;
      int maxDistance = Math.max(32, this.targets.stream()
         .filter(target -> target.dimension().equals(dimension))
         .mapToInt(target -> Math.max(Math.abs(target.x() - playerX), Math.abs(target.z() - playerZ)))
         .max().orElse(32));
      List<Marker> markers = new ArrayList<>(this.targets.size());
      for (BaobhanSithCurseOpenScreenMessage.Target target : this.targets) {
         int baseX;
         int baseY;
         if (target.dimension().equals(dimension)) {
            double normalizedX = 0.5 + (target.x() - playerX) / (double)(maxDistance * 2);
            double normalizedZ = 0.5 + (target.z() - playerZ) / (double)(maxDistance * 2);
            baseX = areaLeft + (int)Math.round(clamp01(normalizedX) * usableWidth);
            baseY = areaTop + (int)Math.round(clamp01(normalizedZ) * usableHeight);
         } else {
            int hash = Math.abs(target.uuid().hashCode());
            double angle = (hash % 360) * Math.PI / 180.0;
            baseX = Math.max(areaLeft, Math.min(areaRight, mapX + mapWidth / 2 + (int)Math.round(Math.cos(angle) * usableWidth * 0.48)));
            baseY = Math.max(areaTop, Math.min(areaBottom, mapY + mapHeight / 2 + (int)Math.round(Math.sin(angle) * usableHeight * 0.48)));
         }
         int markerX = baseX;
         int markerY = baseY;
         for (int attempt = 0; attempt < 128; attempt++) {
            if (attempt > 0) {
               double radius = 3.0 + Math.sqrt(attempt) * 6.0;
               double angle = attempt * 2.399963229728653;
               markerX = Math.max(areaLeft, Math.min(areaRight, baseX + (int)Math.round(Math.cos(angle) * radius)));
               markerY = Math.max(areaTop, Math.min(areaBottom, baseY + (int)Math.round(Math.sin(angle) * radius)));
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
      this.cachedMapWidth = mapWidth;
      this.cachedMapHeight = mapHeight;
      this.cachedPlayerX = playerX;
      this.cachedPlayerZ = playerZ;
      this.cachedDimension = dimension;
      this.cachedMarkers = List.copyOf(markers);
      return this.cachedMarkers;
   }

   private Marker markerAt(double mouseX, double mouseY) {
      for (int i = this.cachedMarkers.size() - 1; i >= 0; i--) {
         Marker marker = this.cachedMarkers.get(i);
         if (mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE) {
            return marker;
         }
      }
      return null;
   }

   private void selectTarget(BaobhanSithCurseOpenScreenMessage.Target target) {
      this.selected = target.uuid();
      if (!this.noblePhantasmMode) {
         PacketDistributor.sendToServer(new BaobhanSithCurseTriggerMessage(target.uuid(), "", true, false, false), new CustomPacketPayload[0]);
      }
   }

   private void clearSelectedTarget() {
      this.selected = null;
   }

   private void detonateTarget(BaobhanSithCurseOpenScreenMessage.Target target) {
      this.selected = target.uuid();
      PacketDistributor.sendToServer(new BaobhanSithCurseTriggerMessage(target.uuid(), "", false, true, false), new CustomPacketPayload[0]);
      this.onClose();
   }

   private void detonateAllTargets() {
      PacketDistributor.sendToServer(new BaobhanSithCurseTriggerMessage(ALL_TARGETS_UUID, "", false, true, true), new CustomPacketPayload[0]);
      this.onClose();
   }

   private BaobhanSithCurseOpenScreenMessage.Target selectedTarget() {
      if (this.selected == null) {
         return null;
      }
      return this.targets.stream().filter(target -> target.uuid().equals(this.selected)).findFirst().orElse(null);
   }

   private int selectedIndex() {
      if (this.selected == null) {
         return 0;
      }
      for (int i = 0; i < this.targets.size(); i++) {
         if (this.targets.get(i).uuid().equals(this.selected)) {
            return i;
         }
      }
      return 0;
   }

   private List<Component> tooltipFor(BaobhanSithCurseOpenScreenMessage.Target target, int playerX, int playerY, int playerZ, String dimension) {
      List<Component> tooltip = new ArrayList<>();
      tooltip.add(Component.literal(target.name()));
      tooltip.add(Component.translatable("screen.typemoonworld.baobhan_sith.target_info",
         target.x(), target.y(), target.z(), Math.round(target.health()), Math.round(target.maxHealth()), target.layers()));
      tooltip.add(Component.translatable("screen.typemoonworld.baobhan_sith.dimension", shortDimension(target.dimension())));
      tooltip.add(target.dimension().equals(dimension)
         ? Component.translatable("screen.typemoonworld.baobhan_sith.distance", Math.round(distanceTo(target, playerX, playerY, playerZ)))
         : Component.translatable("screen.typemoonworld.baobhan_sith.distance_other_dimension"));
      return tooltip;
   }

   private double distanceTo(BaobhanSithCurseOpenScreenMessage.Target target, int x, int y, int z) {
      double dx = target.x() - x;
      double dy = target.y() - y;
      double dz = target.z() - z;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private String shortDimension(String dimension) {
      int index = dimension == null ? -1 : dimension.lastIndexOf(':');
      return index >= 0 ? dimension.substring(index + 1) : String.valueOf(dimension);
   }

   private String trimToWidth(String text, int width) {
      if (this.font.width(text) <= width) {
         return text;
      }
      String ellipsis = "...";
      int limit = Math.max(1, text.length());
      while (limit > 1 && this.font.width(text.substring(0, limit) + ellipsis) > width) {
         limit--;
      }
      return text.substring(0, limit) + ellipsis;
   }

   private double clamp01(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }

   private boolean hasPayload(BaobhanSithCurseOpenScreenMessage.Target target) {
      return target != null && (target.layers() > 0 || target.blood() + target.skin() + target.hair() + target.remains() > 0);
   }

   private void invalidateMarkerCache() {
      this.cachedMarkers = List.of();
      this.cachedMapX = Integer.MIN_VALUE;
   }

   private record Marker(BaobhanSithCurseOpenScreenMessage.Target target, int x, int y) {
   }

   private record MediumButton(BaobhanSithCurseOpenScreenMessage.Target target, String medium, int x, int y) {
      boolean contains(double mouseX, double mouseY) {
         return mouseX >= this.x && mouseX < this.x + MEDIUM_BUTTON_WIDTH && mouseY >= this.y && mouseY < this.y + MEDIUM_BUTTON_HEIGHT;
      }
   }

   private record NoblePhantasmButton(BaobhanSithCurseOpenScreenMessage.Target target, boolean allTargets, int x, int y, int width, int height) {
      boolean contains(double mouseX, double mouseY) {
         return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
      }
   }
}
