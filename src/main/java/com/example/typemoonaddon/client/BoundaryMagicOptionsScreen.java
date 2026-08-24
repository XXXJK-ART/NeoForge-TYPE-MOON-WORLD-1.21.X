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
   private static final int WINDOW_WIDTH = 360;
   private static final int WINDOW_HEIGHT = 320;

   private final Screen parent;
   private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;
   private EditBox sideBox;
   private EditBox complexityBox;
   private EditBox uuidBox;
   private NeonButton powerButton;
   private final Map<String, NeonButton> modeButtons = new LinkedHashMap<>();
   private final List<SelectableTarget> targets = new ArrayList<>();
   private String selectedMode = "none";
   private int selectedPower = 1;

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

      int left = this.width / 2 - WINDOW_WIDTH / 2;
      int top = this.height / 2 - WINDOW_HEIGHT / 2;
      this.sideBox = new EditBox(this.font, left + 22, top + 48, 40, 20, Component.literal("side"));
      this.sideBox.setMaxLength(3);
      this.sideBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,3}"));
      this.sideBox.setValue(Integer.toString(Math.max(10, Math.min(100, preset.contains(BoundaryMagicIntegration.SIDE)
            ? preset.getInt(BoundaryMagicIntegration.SIDE) : 10))));
      this.complexityBox = new EditBox(this.font, left + 72, top + 48, 44, 20, Component.literal("complexity"));
      this.complexityBox.setMaxLength(3);
      this.complexityBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,3}"));
      this.complexityBox.setValue(Integer.toString(Math.max(1, Math.min(100, preset.contains(BoundaryMagicIntegration.COMPLEXITY)
            ? preset.getInt(BoundaryMagicIntegration.COMPLEXITY) : 10))));
      this.uuidBox = new EditBox(this.font, left + 22, top + 150, 188, 20, Component.literal("uuid"));
      this.uuidBox.setMaxLength(36);
      this.uuidBox.setFilter(value -> value.isEmpty() || value.matches("[0-9a-fA-F-]{0,36}"));
      this.uuidBox.setValue(preset.contains(BoundaryMagicIntegration.BLACKLIST_UUID)
            ? preset.getString(BoundaryMagicIntegration.BLACKLIST_UUID) : "");
      this.addRenderableWidget(this.sideBox);
      this.addRenderableWidget(this.complexityBox);
      this.addRenderableWidget(this.uuidBox);

      int buttonY = top + 46;
      int buttonX = left + 120;
      addModeButton("none", buttonX, buttonY, Component.translatable("magic.option.typemoonworld.boundary_blacklist.none"));
      addModeButton("all_players", buttonX + 76, buttonY, Component.translatable("magic.option.typemoonworld.boundary_blacklist.all_players"));
      addModeButton("specific_player", buttonX + 152, buttonY, Component.translatable("magic.option.typemoonworld.boundary_blacklist.specific_player"));
      addModeButton("non_players", buttonX, buttonY + 24, Component.translatable("magic.option.typemoonworld.boundary_blacklist.non_players"));
      addModeButton("all_living", buttonX + 76, buttonY + 24, Component.translatable("magic.option.typemoonworld.boundary_blacklist.all_living"));
      addModeButton("specific_entity", buttonX + 152, buttonY + 24, Component.translatable("magic.option.typemoonworld.boundary_blacklist.specific_entity"));
      addModeButton("hostile", buttonX, buttonY + 48, Component.translatable("magic.option.typemoonworld.boundary_blacklist.hostile"));
      addModeButton("all_aggro_targets", buttonX + 76, buttonY + 48, Component.translatable("magic.option.typemoonworld.boundary_blacklist.all_aggro_targets"));

      this.powerButton = new NeonButton(left + 84, top + 78, 96, 20, powerLabel(), b -> {
         selectedPower = selectedPower >= 5 ? 1 : selectedPower + 1;
         refreshPowerButton();
      }, GuiUtils.ARCANE_CYAN).setArcaneStyle(true).setCompactStyle(true);
      addRenderableWidget(this.powerButton);

      addRenderableWidget(new NeonButton(left + 230, top + 150, 96, 20, Component.translatable("gui.typemoonworld.boundary_options.pick_player"), b -> pickTarget(true), GuiUtils.ARCANE_CYAN)
            .setArcaneStyle(true).setCompactStyle(true));
      addRenderableWidget(new NeonButton(left + 230, top + 174, 96, 20, Component.translatable("gui.typemoonworld.boundary_options.pick_entity"), b -> pickTarget(false), GuiUtils.ARCANE_CYAN)
            .setArcaneStyle(true).setCompactStyle(true));
      addRenderableWidget(new NeonButton(left + 230, top + 198, 96, 20, Component.translatable("gui.typemoonworld.boundary_options.clear_uuid"), b -> this.uuidBox.setValue(""), GuiUtils.ARCANE_DANGER)
            .setArcaneStyle(true).setCompactStyle(true));

      refreshTargets();
      addRenderableWidget(new NeonButton(left + 56, top + 278, 96, 22, Component.translatable("gui.done"), this::save, GuiUtils.ARCANE_VALID)
            .setArcaneStyle(true).setCompactStyle(true));
      addRenderableWidget(new NeonButton(left + 208, top + 278, 96, 22, Component.translatable("gui.cancel"), b -> this.onClose(), GuiUtils.ARCANE_DANGER)
            .setArcaneStyle(true).setCompactStyle(true));
      updateModeButtons();
   }

   private void addModeButton(String mode, int x, int y, Component label) {
      NeonButton button = new NeonButton(x, y, 72, 20, label, b -> {
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

   private Component powerLabel() {
      return Component.translatable("gui.typemoonworld.boundary_options.power", this.selectedPower);
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
      int complexity = parseInt(this.complexityBox == null ? "10" : this.complexityBox.getValue(), 1, 100, 10);
      payload.putInt(BoundaryMagicIntegration.SIDE, side);
      payload.putInt(BoundaryMagicIntegration.COMPLEXITY, complexity);
      payload.putInt(BoundaryMagicIntegration.POWER, this.selectedPower);
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
      GuiUtils.renderArcaneWindow(graphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, GuiUtils.ARCANE_CYAN);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 9, GuiUtils.ARCANE_TEXT);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.side"), left + 22, top + 38, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.complexity"), left + 68, top + 38, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.power_label"), left + 84, top + 70, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.blacklist"), left + 22, top + 120, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.uuid"), left + 22, top + 136, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.targets"), left + 230, top + 120, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, selectedModeLabel(), left + 22, top + 226, GuiUtils.ARCANE_TEXT_MUTED, false);
      graphics.drawString(this.font, Component.translatable("gui.typemoonworld.boundary_options.targets_hint"), left + 230, top + 136, GuiUtils.ARCANE_TEXT_MUTED, false);
      int listY = top + 226;
      int shown = 0;
      for (SelectableTarget target : this.targets) {
         if (shown >= 2) {
            break;
         }
         graphics.drawString(this.font, clampText(target.label.getString(), 122), left + 230,
               listY + shown * 18, GuiUtils.ARCANE_TEXT, false);
         shown++;
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private Component selectedModeLabel() {
      String key = "magic.option.typemoonworld.boundary_blacklist." + this.selectedMode;
      Component translated = Component.translatable(key);
      return translated.getString().equals(key) ? Component.literal(this.selectedMode) : translated;
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
