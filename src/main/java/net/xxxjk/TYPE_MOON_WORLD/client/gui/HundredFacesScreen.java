package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesSummonMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesSwitchMessage;

public final class HundredFacesScreen extends Screen {
   public static final int KIND_SUMMON = 0;
   public static final int KIND_GLOBAL_COMMAND = 1;
   public static final int KIND_SINGLE_COMMAND = 2;
   public static final int KIND_SWITCH = 3;
   private static final int ACCENT = 0xFFB8B5AA;
   private static final int PANEL_WIDTH = 250;
   private static final int MAP_PANEL_WIDTH = 322;
   private static final int MAP_PANEL_HEIGHT = 214;
   private static final int MAP_PADDING = 12;
   private static final int MARKER_SIZE = 14;

   private final int kind;
   private final List<HundredFacesOpenScreenMessage.Target> targets;
   private HundredFacesOpenScreenMessage.Target hoveredTarget;
   private int selectedTargetIndex;
   private List<Marker> cachedMarkers = List.of();
   private int cachedMapX = Integer.MIN_VALUE;
   private int cachedMapY = Integer.MIN_VALUE;
   private int cachedMapWidth = Integer.MIN_VALUE;
   private int cachedMapHeight = Integer.MIN_VALUE;

   public HundredFacesScreen(int kind, List<HundredFacesOpenScreenMessage.Target> targets) {
      super(Component.translatable(kind == KIND_SWITCH ? "screen.typemoonworld.hundred_faces.switch"
         : kind == KIND_SINGLE_COMMAND ? "screen.typemoonworld.hundred_faces.single_command"
         : kind == KIND_GLOBAL_COMMAND ? "screen.typemoonworld.hundred_faces.global_command"
         : "screen.typemoonworld.hundred_faces.summon"));
      this.kind = kind;
      this.targets = targets == null ? List.of() : List.copyOf(targets);
      this.selectedTargetIndex = this.targets.isEmpty() ? -1 : 0;
   }

   @Override
   protected void init() {
      if (this.kind == KIND_SWITCH) return;
      int columns = 2;
      int buttonWidth = 108;
      int buttonHeight = 22;
      int gap = 8;
      int startX = (this.width - (buttonWidth * columns + gap)) / 2;
      int startY = this.height / 2 - (this.choiceCount() > 4 ? 38 : 24);

      if (this.kind == KIND_SUMMON) {
         int[] requests = {1, 5, 10, 20, 80};
         String[] keys = {"one", "five", "ten", "twenty", "max"};
         for (int i = 0; i < requests.length; i++) {
            int request = requests[i];
            int x = startX + i % columns * (buttonWidth + gap);
            int y = startY + i / columns * (buttonHeight + gap);
            this.addRenderableWidget(new NeonButton(x, y, buttonWidth, buttonHeight,
               Component.translatable("screen.typemoonworld.hundred_faces.summon." + keys[i]),
               button -> this.selectSummon(request), i % 2 == 0 ? GuiUtils.ARCANE_CREST : ACCENT).setArcaneStyle(true));
         }
         return;
      }

      if (this.kind == KIND_GLOBAL_COMMAND) {
         int[] commands = {
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_RECALL,
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_SCATTER,
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_ATTACK_TOGGLE
         };
         String[] keys = {"recall", "scatter", "attack_toggle"};
         for (int i = 0; i < commands.length; i++) {
            int command = commands[i];
            int x = startX + i % columns * (buttonWidth + gap);
            int y = startY + i / columns * (buttonHeight + gap);
            this.addRenderableWidget(new NeonButton(x, y, buttonWidth, buttonHeight,
               Component.translatable("screen.typemoonworld.hundred_faces.command." + keys[i]),
               button -> this.selectCommand(0, -1, command), i == 2 ? GuiUtils.ARCANE_GOLD : ACCENT).setArcaneStyle(true));
         }
         return;
      }

      int entityId = this.targets.isEmpty() ? -1 : this.targets.get(0).entityId();
      int[] commands = {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_HOLD,
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_FREE,
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_FOLLOW,
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_ATTACK_TOGGLE
      };
      String[] keys = {"hold", "free", "follow", "attack_toggle"};
      int[] colors = {GuiUtils.ARCANE_GOLD, GuiUtils.ARCANE_VALID, ACCENT, 0xFFD0B05C};
      for (int i = 0; i < commands.length; i++) {
         int command = commands[i];
         int x = startX + i % columns * (buttonWidth + gap);
         int y = startY + i / columns * (buttonHeight + gap);
         this.addRenderableWidget(new NeonButton(x, y, buttonWidth, buttonHeight,
            Component.translatable("screen.typemoonworld.hundred_faces.command." + keys[i]),
            button -> this.selectCommand(1, entityId, command), colors[i]).setArcaneStyle(true));
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
      if (this.kind == KIND_SWITCH) this.renderSwitchMap(gui, mouseX, mouseY);
      else this.renderChoicePanel(gui);
      super.render(gui, mouseX, mouseY, partialTick);
      if (this.hoveredTarget != null) {
         gui.renderTooltip(this.font, List.of(
            Component.literal(this.hoveredTarget.name()),
            Component.translatable("screen.typemoonworld.hundred_faces.coordinates", this.hoveredTarget.x(), this.hoveredTarget.y(), this.hoveredTarget.z()),
            Component.translatable("screen.typemoonworld.hundred_faces.stats", this.hoveredTarget.health(), this.hoveredTarget.maxHealth(), this.hoveredTarget.armor()),
            Component.translatable("screen.typemoonworld.hundred_faces.mode", this.hoveredTarget.mode()),
            Component.translatable("screen.typemoonworld.hundred_faces.command_state",
               commandName(this.hoveredTarget.command()),
               this.hoveredTarget.attackEnabled()
                  ? Component.translatable("screen.typemoonworld.hundred_faces.on")
                  : Component.translatable("screen.typemoonworld.hundred_faces.off"))
         ), java.util.Optional.empty(), mouseX, mouseY);
      }
   }

   @Override
   public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
   }

   private void renderChoicePanel(GuiGraphics gui) {
      int panelHeight = this.choiceCount() > 4 ? 138 : 110;
      int panelX = (this.width - PANEL_WIDTH) / 2;
      int panelY = (this.height - panelHeight) / 2 - 8;
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, PANEL_WIDTH, panelHeight, ACCENT);
      gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
      if (this.kind == KIND_SINGLE_COMMAND && this.targets.isEmpty()) {
         gui.drawCenteredString(this.font, Component.translatable("screen.typemoonworld.hundred_faces.no_targets"),
            this.width / 2, panelY + 36, GuiUtils.ARCANE_TEXT_MUTED);
      }
   }

   private void renderSwitchMap(GuiGraphics gui, int mouseX, int mouseY) {
      int panelX = (this.width - MAP_PANEL_WIDTH) / 2;
      int panelY = (this.height - MAP_PANEL_HEIGHT) / 2;
      int mapX = panelX + 12;
      int mapY = panelY + 37;
      int mapWidth = MAP_PANEL_WIDTH - 24;
      int mapHeight = MAP_PANEL_HEIGHT - 50;
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, MAP_PANEL_WIDTH, MAP_PANEL_HEIGHT, ACCENT);
      GuiUtils.renderArcanePanel(gui, mapX, mapY, mapWidth, mapHeight, ACCENT);
      gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
      Component count = Component.translatable("screen.typemoonworld.hundred_faces.target_count", this.targets.size());
      gui.drawString(this.font, count, panelX + MAP_PANEL_WIDTH - 12 - this.font.width(count), panelY + 9, GuiUtils.ARCANE_TEXT_MUTED, false);
      this.renderMapGrid(gui, mapX, mapY, mapWidth, mapHeight);
      this.hoveredTarget = null;
      if (this.targets.isEmpty()) {
         gui.drawCenteredString(this.font, Component.translatable("screen.typemoonworld.hundred_faces.no_targets"),
            mapX + mapWidth / 2, mapY + mapHeight / 2 - 4, GuiUtils.ARCANE_TEXT_MUTED);
         return;
      }

      List<Marker> markers = this.layoutMarkers(mapX, mapY, mapWidth, mapHeight);
      for (int i = 0; i < markers.size(); i++) {
         Marker marker = markers.get(i);
         boolean hovered = mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE;
         boolean selected = i == this.selectedTargetIndex;
         int accent = hovered ? GuiUtils.ARCANE_VALID : selected ? GuiUtils.ARCANE_GOLD : ACCENT;
         int fill = hovered || selected ? 0xF0262624 : GuiUtils.ARCANE_PANEL_ALT;
         gui.fill(marker.x, marker.y, marker.x + MARKER_SIZE, marker.y + MARKER_SIZE, fill);
         gui.renderOutline(marker.x, marker.y, MARKER_SIZE, MARKER_SIZE, accent);
         gui.fill(marker.x + 2, marker.y + 2, marker.x + MARKER_SIZE - 2, marker.y + 4, accent);
         gui.fill(marker.x + 5, marker.y + 6, marker.x + 9, marker.y + 10, accent);
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
         gui.fill(gridX, top, gridX + 1, bottom, 0x203C3930);
         gui.fill(left, gridY, right, gridY + 1, 0x203C3930);
      }
      gui.fill((left + right) / 2, top, (left + right) / 2 + 1, bottom, 0x304E4A3F);
      gui.fill(left, (top + bottom) / 2, right, (top + bottom) / 2 + 1, 0x304E4A3F);
   }

   private List<Marker> layoutMarkers(int mapX, int mapY, int mapWidth, int mapHeight) {
      if (mapX == this.cachedMapX && mapY == this.cachedMapY && mapWidth == this.cachedMapWidth && mapHeight == this.cachedMapHeight) {
         return this.cachedMarkers;
      }
      int minX = this.targets.stream().mapToInt(HundredFacesOpenScreenMessage.Target::x).min().orElse(0);
      int maxX = this.targets.stream().mapToInt(HundredFacesOpenScreenMessage.Target::x).max().orElse(minX);
      int minZ = this.targets.stream().mapToInt(HundredFacesOpenScreenMessage.Target::z).min().orElse(0);
      int maxZ = this.targets.stream().mapToInt(HundredFacesOpenScreenMessage.Target::z).max().orElse(minZ);
      int usableWidth = mapWidth - MAP_PADDING * 2 - MARKER_SIZE;
      int usableHeight = mapHeight - MAP_PADDING * 2 - MARKER_SIZE;
      int areaLeft = mapX + MAP_PADDING;
      int areaTop = mapY + MAP_PADDING;
      int areaRight = areaLeft + usableWidth;
      int areaBottom = areaTop + usableHeight;
      List<Marker> markers = new ArrayList<>(this.targets.size());
      for (HundredFacesOpenScreenMessage.Target target : this.targets) {
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
      if (this.kind == KIND_SWITCH && button == 0) {
         int panelX = (this.width - MAP_PANEL_WIDTH) / 2;
         int panelY = (this.height - MAP_PANEL_HEIGHT) / 2;
         int mapX = panelX + 12;
         int mapY = panelY + 37;
         int mapWidth = MAP_PANEL_WIDTH - 24;
         int mapHeight = MAP_PANEL_HEIGHT - 50;
         List<Marker> markers = this.layoutMarkers(mapX, mapY, mapWidth, mapHeight);
         for (int i = markers.size() - 1; i >= 0; i--) {
            Marker marker = markers.get(i);
            if (mouseX >= marker.x && mouseX < marker.x + MARKER_SIZE && mouseY >= marker.y && mouseY < marker.y + MARKER_SIZE) {
               this.selectedTargetIndex = i;
               this.selectSwitch(marker.target.entityId());
               return true;
            }
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.kind == KIND_SWITCH && !this.targets.isEmpty()) {
         if (keyCode == 262 || keyCode == 264) {
            this.selectedTargetIndex = (this.selectedTargetIndex + 1) % this.targets.size();
            return true;
         }
         if (keyCode == 263 || keyCode == 265) {
            this.selectedTargetIndex = Math.floorMod(this.selectedTargetIndex - 1, this.targets.size());
            return true;
         }
         if (keyCode == 257 || keyCode == 335) {
            this.selectSwitch(this.targets.get(this.selectedTargetIndex).entityId());
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
      return this.kind == KIND_SUMMON ? 5 : this.kind == KIND_GLOBAL_COMMAND ? 3 : 4;
   }

   private void selectSummon(int requested) {
      PacketDistributor.sendToServer(new HundredFacesSummonMessage(requested), new CustomPacketPayload[0]);
      this.onClose();
   }

   private void selectCommand(int scope, int entityId, int command) {
      PacketDistributor.sendToServer(new HundredFacesCommandMessage(scope, entityId, command), new CustomPacketPayload[0]);
      this.onClose();
   }

   private void selectSwitch(int id) {
      PacketDistributor.sendToServer(new HundredFacesSwitchMessage(id), new CustomPacketPayload[0]);
      this.onClose();
   }

   private static Component commandName(int command) {
      String key = switch (command) {
         case net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_HOLD -> "hold";
         case net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.COMMAND_FOLLOW -> "follow";
         default -> "free";
      };
      return Component.translatable("screen.typemoonworld.hundred_faces.command." + key);
   }

   private record Marker(HundredFacesOpenScreenMessage.Target target, int x, int y) {
   }
}
