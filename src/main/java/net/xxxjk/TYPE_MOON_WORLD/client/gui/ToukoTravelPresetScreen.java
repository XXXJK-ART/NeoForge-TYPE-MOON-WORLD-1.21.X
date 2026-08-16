package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

public final class ToukoTravelPresetScreen extends Screen {
   private static final int BOX_W = 260;
   private static final int BOX_H = 162;
   private final Screen parent;
   private final Magical_attributes_Screen.MagicEntry entry;
   private final int targetSlot;
   private final CompoundTag initialPayload;
   private EditBox xBox;
   private EditBox yBox;
   private EditBox zBox;

   public ToukoTravelPresetScreen(Screen parent, Magical_attributes_Screen.MagicEntry entry, int targetSlot, CompoundTag initialPayload) {
      super(Component.translatable("gui.typemoonworld.touko_travel.title"));
      this.parent = parent;
      this.entry = entry;
      this.targetSlot = targetSlot;
      this.initialPayload = initialPayload == null ? new CompoundTag() : initialPayload.copy();
   }

   @Override
   protected void init() {
      int x = (this.width - BOX_W) / 2;
      int y = (this.height - BOX_H) / 2;
      this.xBox = coordinateBox(x + 24, y + 48, "x");
      this.yBox = coordinateBox(x + 98, y + 48, "y");
      this.zBox = coordinateBox(x + 172, y + 48, "z");
      this.addRenderableWidget(this.xBox);
      this.addRenderableWidget(this.yBox);
      this.addRenderableWidget(this.zBox);
      double initialX = this.initialPayload.contains("x") ? this.initialPayload.getDouble("x") : currentPlayerX();
      double initialY = this.initialPayload.contains("y") ? this.initialPayload.getDouble("y") : currentPlayerY();
      double initialZ = this.initialPayload.contains("z") ? this.initialPayload.getDouble("z") : currentPlayerZ();
      setBoxes(initialX, initialY, initialZ);
      this.setInitialFocus(this.xBox);
      this.addRenderableWidget(
         new NeonButton(x + 24, y + 78, 66, 18, Component.translatable("gui.typemoonworld.touko_travel.current"), b -> setCurrent(), GuiUtils.ARCANE_CYAN)
            .setArcaneStyle(true)
            .setCompactStyle(true)
      );
      this.addRenderableWidget(
         new NeonButton(x + 98, y + 78, 66, 18, Component.translatable("gui.typemoonworld.touko_travel.looking"), b -> setLooking(), GuiUtils.ARCANE_GOLD)
            .setArcaneStyle(true)
            .setCompactStyle(true)
      );
      this.addRenderableWidget(
         new NeonButton(x + 172, y + 78, 66, 18, Component.translatable("gui.typemoonworld.touko_travel.clamp"), b -> clampToRange(), GuiUtils.ARCANE_CREST)
            .setArcaneStyle(true)
            .setCompactStyle(true)
      );
      this.addRenderableWidget(
         new NeonButton(x + 42, y + 122, 76, 20, Component.translatable("gui.done"), b -> save(), GuiUtils.ARCANE_VALID)
            .setArcaneStyle(true)
            .setCompactStyle(true)
      );
      this.addRenderableWidget(
         new NeonButton(x + 142, y + 122, 76, 20, Component.translatable("gui.cancel"), b -> onClose(), GuiUtils.ARCANE_DANGER)
            .setArcaneStyle(true)
            .setCompactStyle(true)
      );
   }

   private EditBox coordinateBox(int x, int y, String axis) {
      EditBox box = new EditBox(this.font, x, y, 64, 18, Component.literal(axis));
      box.setMaxLength(16);
      box.setFilter(value -> value.isEmpty() || value.matches("-?[0-9]{0,7}(\\.[0-9]{0,3})?"));
      box.setTextColor(GuiUtils.ARCANE_TEXT);
      return box;
   }

   private double currentPlayerX() {
      return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getX() : 0.0;
   }

   private double currentPlayerY() {
      return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getY() : 64.0;
   }

   private double currentPlayerZ() {
      return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getZ() : 0.0;
   }

   private void setCurrent() {
      if (this.minecraft != null && this.minecraft.player != null) {
         setBoxes(this.minecraft.player.getX(), this.minecraft.player.getY(), this.minecraft.player.getZ());
      }
   }

   private void setLooking() {
      if (this.minecraft != null && this.minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
         setBoxes(hit.getBlockPos().getX() + 0.5, hit.getBlockPos().getY() + 1.0, hit.getBlockPos().getZ() + 0.5);
      }
   }

   private void clampToRange() {
      if (this.minecraft == null || this.minecraft.player == null) {
         return;
      }
      double x = read(this.xBox, this.minecraft.player.getX());
      double y = read(this.yBox, this.minecraft.player.getY());
      double z = read(this.zBox, this.minecraft.player.getZ());
      double dx = x - this.minecraft.player.getX();
      double dy = y - this.minecraft.player.getY();
      double dz = z - this.minecraft.player.getZ();
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (distance > 1000.0) {
         double scale = 1000.0 / distance;
         setBoxes(this.minecraft.player.getX() + dx * scale, this.minecraft.player.getY() + dy * scale, this.minecraft.player.getZ() + dz * scale);
      }
   }

   private void setBoxes(double x, double y, double z) {
      this.xBox.setValue(format(x));
      this.yBox.setValue(format(y));
      this.zBox.setValue(format(z));
   }

   private static String format(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }

   private static double read(EditBox box, double fallback) {
      try {
         return Double.parseDouble(box.getValue());
      } catch (NumberFormatException ignored) {
         return fallback;
      }
   }

   private void save() {
      if (this.minecraft == null || this.minecraft.player == null || this.entry == null) {
         onClose();
         return;
      }
      Player player = this.minecraft.player;
      double x = read(this.xBox, player.getX());
      double y = Mth.clamp(read(this.yBox, player.getY()), player.level().getMinBuildHeight(), player.level().getMaxBuildHeight() - 1);
      double z = read(this.zBox, player.getZ());
      if (player.distanceToSqr(x, y, z) > 1000.0 * 1000.0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.touko_travel.too_far"), true);
         return;
      }
      CompoundTag payload = new CompoundTag();
      payload.putDouble("x", x);
      payload.putDouble("y", y);
      payload.putDouble("z", z);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int wheel = vars.active_wheel_index;
      String displayName = Component.translatable(this.entry.nameKey).getString() + String.format(Locale.ROOT, " %.1f %.1f %.1f", x, y, z);
      PacketDistributor.sendToServer(
         new MagicWheelSlotEditMessage(0, wheel, this.targetSlot, -1, this.entry.sourceType, this.entry.id, payload, this.entry.crestEntryId, displayName),
         new CustomPacketPayload[0]
      );
      onClose();
   }

   @Override
   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int x = (this.width - BOX_W) / 2;
      int y = (this.height - BOX_H) / 2;
      GuiUtils.renderScreenBackdrop(guiGraphics, this.width, this.height);
      GuiUtils.renderArcaneWindow(guiGraphics, x, y, BOX_W, BOX_H, GuiUtils.ARCANE_CYAN);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, y + 9, GuiUtils.ARCANE_TEXT);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.touko_travel.subtitle"), this.width / 2, y + 28, GuiUtils.ARCANE_TEXT_MUTED);
      guiGraphics.drawString(this.font, "X", x + 24, y + 38, GuiUtils.ARCANE_TEXT_MUTED, false);
      guiGraphics.drawString(this.font, "Y", x + 98, y + 38, GuiUtils.ARCANE_TEXT_MUTED, false);
      guiGraphics.drawString(this.font, "Z", x + 172, y + 38, GuiUtils.ARCANE_TEXT_MUTED, false);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      return this.xBox != null && this.xBox.charTyped(codePoint, modifiers)
         || this.yBox != null && this.yBox.charTyped(codePoint, modifiers)
         || this.zBox != null && this.zBox.charTyped(codePoint, modifiers)
         || super.charTyped(codePoint, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 257 || keyCode == 335) {
         save();
         return true;
      }
      if (keyCode == 256) {
         onClose();
         return true;
      }
      return this.xBox != null && this.xBox.keyPressed(keyCode, scanCode, modifiers)
         || this.yBox != null && this.yBox.keyPressed(keyCode, scanCode, modifiers)
         || this.zBox != null && this.zBox.keyPressed(keyCode, scanCode, modifiers)
         || super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   @Override
   public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
