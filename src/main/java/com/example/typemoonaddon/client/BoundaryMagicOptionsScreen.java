package com.example.typemoonaddon.client;

import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.NeonButton;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class BoundaryMagicOptionsScreen extends Screen {
   private static final int WINDOW_WIDTH = 420;
   private static final int WINDOW_HEIGHT = 326;
   private static final int PANEL_TOP = 38;
   private static final int PANEL_GAP = 10;
   private static final int BASIC_PANEL_WIDTH = 128;
   private static final int BLACKLIST_PANEL_WIDTH = 250;
   private static final int TOP_PANEL_HEIGHT = 136;
   private static final int TARGET_PANEL_TOP = 180;
   private static final int TARGET_PANEL_HEIGHT = 92;
   private static final int ACTION_BUTTON_TOP = 274;
   private static final int BUTTON_HEIGHT = 18;

   private final Screen parent;
   private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;
   private EditBox sideBox;
   private EditBox uuidBox;
   private NeonButton powerButton;
   private NeonButton shapeButton;
   private NeonButton debuffButton;
   private final Map<String, NeonButton> modeButtons = new LinkedHashMap<>();
   private final List<SelectableTarget> targets = new ArrayList<>();
   private String selectedMode = "none";
   private int selectedPower = 1;
   private String selectedShape = "sphere";
   private String selectedDebuff = "nausea";

   public BoundaryMagicOptionsScreen(Screen parent) {
      super(Component.translatable("gui.typemoonworld.boundary_options.title"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      if (this.minecraft == null || this.minecraft.player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = this.minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      this.entry = vars.getCurrentRuntimeWheelEntry();
      if (this.entry == null || this.entry.magicId == null || this.entry.magicId.isEmpty()
            || !BoundaryMagicIntegration.isBoundaryMagicId(this.entry.magicId)) {
         this.onClose();
         return;
      }

      CompoundTag preset = this.entry.presetPayload == null ? new CompoundTag() : this.entry.presetPayload.copy();
      this.selectedMode = preset.contains(BoundaryMagicIntegration.BLACKLIST)
            ? preset.getString(BoundaryMagicIntegration.BLACKLIST)
            : "none";
      this.selectedPower = Math.max(1, Math.min(5, preset.contains(BoundaryMagicIntegration.POWER)
            ? preset.getInt(BoundaryMagicIntegration.POWER) : 1));
      this.selectedShape = normalizeShape(preset.contains(BoundaryMagicIntegration.SHAPE)
            ? preset.getString(BoundaryMagicIntegration.SHAPE) : "sphere");
      this.selectedDebuff = preset.contains(BoundaryMagicIntegration.INTERFERENCE_EFFECT)
            ? normalizeDebuff(preset.getString(BoundaryMagicIntegration.INTERFERENCE_EFFECT)) : "nausea";

      int left = this.width / 2 - WINDOW_WIDTH / 2;
      int top = this.height / 2 - WINDOW_HEIGHT / 2;
      int basicX = left + 16;
      int blacklistX = basicX + BASIC_PANEL_WIDTH + PANEL_GAP;
      int targetX = left + 16;
      int targetY = top + TARGET_PANEL_TOP;

      this.sideBox = new EditBox(this.font, basicX + 12, top + 70, 42, 18, Component.literal("side"));
      this.sideBox.setMaxLength(3);
      this.sideBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,3}"));
      this.sideBox.setValue(Integer.toString(Math.max(10, Math.min(100, preset.contains(BoundaryMagicIntegration.SIDE)
            ? preset.getInt(BoundaryMagicIntegration.SIDE) : 10))));
      this.sideBox.setTextColor(GuiUtils.ARCANE_TEXT);
      this.uuidBox = new EditBox(this.font, targetX + 12, targetY + 30, 236, 18, Component.literal("uuid"));
      this.uuidBox.setMaxLength(36);
      this.uuidBox.setFilter(value -> value.isEmpty() || value.matches("[0-9a-fA-F-]{0,36}"));
      this.uuidBox.setValue(preset.contains(BoundaryMagicIntegration.BLACKLIST_UUID)
            ? preset.getString(BoundaryMagicIntegration.BLACKLIST_UUID) : "");
      this.uuidBox.setTextColor(GuiUtils.ARCANE_TEXT);
      this.addRenderableWidget(this.sideBox);
      this.addRenderableWidget(this.uuidBox);

      int modeX = blacklistX + 12;
      int modeY = top + 62;
      int modeW = 111;
      int modeGap = 8;
      int row = 21;
      addModeButton("none", modeX, modeY, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.none"));
      addModeButton("all_players", modeX + modeW + modeGap, modeY, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.all_players"));
      addModeButton("specific_player", modeX, modeY + row, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.specific_player"));
      addModeButton("non_players", modeX + modeW + modeGap, modeY + row, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.non_players"));
      addModeButton("all_living", modeX, modeY + row * 2, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.all_living"));
      addModeButton("specific_entity", modeX + modeW + modeGap, modeY + row * 2, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.specific_entity"));
      addModeButton("hostile", modeX, modeY + row * 3, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.hostile"));
      addModeButton("all_aggro_targets", modeX + modeW + modeGap, modeY + row * 3, modeW, Component.translatable("magic.option.typemoonworld.boundary_blacklist.all_aggro_targets"));

      this.powerButton = new NeonButton(basicX + 12, top + 112, 102, BUTTON_HEIGHT, powerLabel(), b -> {
         selectedPower = selectedPower >= 5 ? 1 : selectedPower + 1;
         refreshPowerButton();
      }, GuiUtils.ARCANE_CYAN).setArcaneStyle(true).setCompactStyle(true);
      addRenderableWidget(this.powerButton);

      this.shapeButton = new NeonButton(basicX + 12, top + 132, 102, BUTTON_HEIGHT, shapeLabel(), b -> {
         selectedShape = "sphere".equals(selectedShape) ? "hemisphere" : "sphere";
         refreshShapeButton();
      }, GuiUtils.ARCANE_CREST).setArcaneStyle(true).setCompactStyle(true);
      addRenderableWidget(this.shapeButton);

      this.debuffButton = new NeonButton(basicX + 12, top + 153, 102, BUTTON_HEIGHT, debuffLabel(), b -> {
         selectedDebuff = switch (selectedDebuff) {
            case "nausea" -> "blindness";
            case "blindness" -> "darkness";
            default -> "nausea";
         };
         refreshDebuffButton();
      }, GuiUtils.ARCANE_CREST).setArcaneStyle(true).setCompactStyle(true);
      this.debuffButton.visible = isInterferenceMagic();
      addRenderableWidget(this.debuffButton);

      int targetButtonX = targetX + 264;
      addRenderableWidget(new NeonButton(targetButtonX, targetY + 22, 104, BUTTON_HEIGHT, Component.translatable("gui.typemoonworld.boundary_options.pick_player"), b -> pickTarget(true), GuiUtils.ARCANE_CYAN)
            .setArcaneStyle(true).setCompactStyle(true));
      addRenderableWidget(new NeonButton(targetButtonX, targetY + 43, 104, BUTTON_HEIGHT, Component.translatable("gui.typemoonworld.boundary_options.pick_entity"), b -> pickTarget(false), GuiUtils.ARCANE_CYAN)
            .setArcaneStyle(true).setCompactStyle(true));
      addRenderableWidget(new NeonButton(targetButtonX, targetY + 64, 104, BUTTON_HEIGHT, Component.translatable("gui.typemoonworld.boundary_options.clear_uuid"), b -> this.uuidBox.setValue(""), GuiUtils.ARCANE_DANGER)
            .setArcaneStyle(true).setCompactStyle(true));

      refreshTargets();
      addRenderableWidget(new NeonButton(left + 104, top + ACTION_BUTTON_TOP, 96, 20, Component.translatable("gui.done"), this::save, GuiUtils.ARCANE_VALID)
            .setArcaneStyle(true).setCompactStyle(true));
      addRenderableWidget(new NeonButton(left + 220, top + ACTION_BUTTON_TOP, 96, 20, Component.translatable("gui.cancel"), b -> this.onClose(), GuiUtils.ARCANE_DANGER)
            .setArcaneStyle(true).setCompactStyle(true));
      updateModeButtons();
   }

   private void addModeButton(String mode, int x, int y, int width, Component label) {
      NeonButton button = new NeonButton(x, y, width, BUTTON_HEIGHT, label, b -> {
         this.selectedMode = mode;
         updateModeButtons();
      }, GuiUtils.ARCANE_GOLD).setArcaneStyle(true).setCompactStyle(true);
      this.modeButtons.put(mode, button);
      this.addRenderableWidget(button);
   }

   private void updateModeButtons() {
      this.modeButtons.forEach((mode, button) -> button.setSelected(mode.equals(this.selectedMode)));
   }

   private void refreshPowerButton() {
      if (this.powerButton != null) {
         this.powerButton.setMessage(powerLabel());
      }
   }

   private void refreshDebuffButton() {
      if (this.debuffButton != null) {
         this.debuffButton.setMessage(debuffLabel());
      }
   }

   private void refreshShapeButton() {
      if (this.shapeButton != null) {
         this.shapeButton.setMessage(shapeLabel());
      }
   }

   private Component powerLabel() {
      return Component.translatable("gui.typemoonworld.boundary_options.power", this.selectedPower);
   }

   private Component debuffLabel() {
      return Component.translatable("gui.typemoonworld.boundary_options.debuff." + this.selectedDebuff);
   }

   private Component shapeLabel() {
      return Component.translatable("gui.typemoonworld.boundary_options.shape." + this.selectedShape);
   }

   private void refreshTargets() {
      this.targets.clear();
      if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
         return;
      }
      Player player = this.minecraft.player;
      Map<UUID, SelectableTarget> collected = new LinkedHashMap<>();
      for (Player nearbyPlayer : this.minecraft.level.players()) {
         if (nearbyPlayer != player) {
            collected.putIfAbsent(nearbyPlayer.getUUID(), new SelectableTarget(nearbyPlayer.getUUID(), nearbyPlayer.getDisplayName(), true));
         }
      }
      this.minecraft.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40.0D),
               entity -> entity != player && entity.isAlive())
            .stream()
            .sorted(Comparator.comparingDouble(player::distanceToSqr))
            .forEach(entity -> collected.putIfAbsent(entity.getUUID(),
                  new SelectableTarget(entity.getUUID(), entity.getDisplayName(), false)));
      this.targets.addAll(collected.values());
   }

   private void pickTarget(boolean playerOnly) {
      refreshTargets();
      for (SelectableTarget target : this.targets) {
         if (playerOnly && !target.player) {
            continue;
         }
         this.uuidBox.setValue(target.id.toString());
         this.selectedMode = target.player ? "specific_player" : "specific_entity";
         updateModeButtons();
         break;
      }
   }

   private void save(Button ignored) {
      if (this.minecraft == null || this.minecraft.player == null || this.entry == null) {
         this.onClose();
         return;
      }
      CompoundTag payload = this.entry.presetPayload == null ? new CompoundTag() : this.entry.presetPayload.copy();
      int side = parseInt(this.sideBox == null ? "10" : this.sideBox.getValue(), 10, 100, 10);
      payload.putInt(BoundaryMagicIntegration.SIDE, side);
      payload.putInt(BoundaryMagicIntegration.POWER, this.selectedPower);
      payload.putString(BoundaryMagicIntegration.SHAPE, normalizeShape(this.selectedShape));
      if (isInterferenceMagic()) {
         payload.putString(BoundaryMagicIntegration.INTERFERENCE_EFFECT, normalizeDebuff(this.selectedDebuff));
      } else {
         payload.remove(BoundaryMagicIntegration.INTERFERENCE_EFFECT);
      }
      payload.putString(BoundaryMagicIntegration.BLACKLIST, this.selectedMode);
      if ("specific_player".equals(this.selectedMode) || "specific_entity".equals(this.selectedMode)) {
         String raw = this.uuidBox == null ? "" : this.uuidBox.getValue().trim();
         if (!raw.isEmpty()) {
            try {
               payload.putString(BoundaryMagicIntegration.BLACKLIST_UUID, UUID.fromString(raw).toString());
            } catch (IllegalArgumentException ignoredException) {
               payload.remove(BoundaryMagicIntegration.BLACKLIST_UUID);
            }
         }
      } else {
         payload.remove(BoundaryMagicIntegration.BLACKLIST_UUID);
      }

      PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(MagicWheelSlotEditMessage.ACTION_SET,
            this.entry.wheelIndex, this.entry.slotIndex, -1, this.entry.sourceType, this.entry.magicId, payload,
            this.entry.crestEntryId, this.entry.displayNameCache), new CustomPacketPayload[0]);
      this.onClose();
   }

   private static int parseInt(String text, int min, int max, int fallback) {
      try {
         return Math.max(min, Math.min(max, Integer.parseInt(text)));
      } catch (NumberFormatException ex) {
         return fallback;
      }
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, this.width, this.height);
      int left = (this.width - WINDOW_WIDTH) / 2;
      int top = (this.height - WINDOW_HEIGHT) / 2;
      int basicX = left + 16;
      int blacklistX = basicX + BASIC_PANEL_WIDTH + PANEL_GAP;
      int targetX = left + 16;
      int targetY = top + TARGET_PANEL_TOP;
      GuiUtils.renderArcaneWindow(graphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, GuiUtils.ARCANE_CYAN);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 9, GuiUtils.ARCANE_TEXT);

      GuiUtils.renderArcanePanel(graphics, basicX, top + PANEL_TOP, BASIC_PANEL_WIDTH, TOP_PANEL_HEIGHT, GuiUtils.ARCANE_CYAN);
      GuiUtils.renderArcanePanel(graphics, blacklistX, top + PANEL_TOP, BLACKLIST_PANEL_WIDTH, TOP_PANEL_HEIGHT, GuiUtils.ARCANE_GOLD);
      GuiUtils.renderArcanePanel(graphics, targetX, targetY, WINDOW_WIDTH - 32, TARGET_PANEL_HEIGHT, GuiUtils.ARCANE_CREST);

      drawPanelTitle(graphics, Component.translatable("gui.typemoonworld.boundary_options.parameters"), basicX + 10, top + 45, BASIC_PANEL_WIDTH - 20, GuiUtils.ARCANE_CYAN);
      drawPanelTitle(graphics, Component.translatable("gui.typemoonworld.boundary_options.blacklist"), blacklistX + 10, top + 45, BLACKLIST_PANEL_WIDTH - 20, GuiUtils.ARCANE_GOLD);
      drawPanelTitle(graphics, Component.translatable("gui.typemoonworld.boundary_options.targeting"), targetX + 10, targetY + 8, WINDOW_WIDTH - 52, GuiUtils.ARCANE_CREST);

      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.side"), basicX + 12, top + 60, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.power_label"), basicX + 12, top + 100, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.shape"), basicX + 12, top + 122, GuiUtils.ARCANE_TEXT_MUTED, false);
      if (isInterferenceMagic()) {
         graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.debuff"), basicX + 12, top + 143, GuiUtils.ARCANE_TEXT_MUTED, false);
      }
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.uuid"), targetX + 12, targetY + 20, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.targets"), targetX + 12, targetY + 55, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.selected", selectedModeLabel()), blacklistX + 12,
            top + 131, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.targets_hint"), targetX + 264, targetY + 10, GuiUtils.ARCANE_TEXT_MUTED, false);
      int listY = targetY + 70;
      int shown = 0;
      for (SelectableTarget target : this.targets) {
         if (shown >= 2) {
            break;
         }
         graphics.drawString(this.font, clampText(target.label.getString(), 220), targetX + 12,
               listY + shown * 10, GuiUtils.ARCANE_TEXT, false);
         shown++;
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void drawPanelTitle(GuiGraphics graphics, Component title, int x, int y, int width, int accentColor) {
      GuiUtils.renderSectionHeader(graphics, x, y + 2, width, accentColor);
      graphics.drawString(this.font, title, x, y, GuiUtils.ARCANE_TEXT, false);
   }

   private Component selectedModeLabel() {
      String key = "magic.option.typemoonworld.boundary_blacklist." + this.selectedMode;
      Component translated = Component.translatable(key);
      return translated.getString().equals(key) ? Component.literal(this.selectedMode) : translated;
   }

   private boolean isInterferenceMagic() {
      return this.entry != null && this.entry.magicId != null && this.entry.magicId.endsWith("interference_boundary");
   }

   private static String normalizeDebuff(String value) {
      return switch (value) {
         case "blindness", "darkness" -> value;
         default -> "nausea";
      };
   }

   private static String normalizeShape(String value) {
      return "hemisphere".equals(value) ? "hemisphere" : "sphere";
   }

   private String clampText(String text, int maxWidth) {
      if (this.font.width(text) <= maxWidth) {
         return text;
      }
      String suffix = "...";
      while (!text.isEmpty() && this.font.width(text + suffix) > maxWidth) {
         text = text.substring(0, text.length() - 1);
      }
      return text + suffix;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private record SelectableTarget(UUID id, Component label, boolean player) {
   }
}
