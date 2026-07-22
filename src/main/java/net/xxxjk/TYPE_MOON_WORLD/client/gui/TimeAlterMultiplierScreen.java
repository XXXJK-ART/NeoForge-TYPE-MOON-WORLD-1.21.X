package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SetTimeAlterMultiplierMessage;

public final class TimeAlterMultiplierScreen extends Screen {
   private final int initialMultiplier;
   private EditBox multiplier;

   public TimeAlterMultiplierScreen(int initialMultiplier) {
      super(Component.translatable("gui.typemoonworld.time_alter.title"));
      this.initialMultiplier = Math.max(1, initialMultiplier);
   }

   @Override
   protected void init() {
      int x = this.width / 2 - 80;
      int y = this.height / 2 - 42;
      this.multiplier = new EditBox(this.font, x, y, 160, 20, Component.translatable("gui.typemoonworld.time_alter.multiplier"));
      this.multiplier.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,10}"));
      this.multiplier.setValue(Integer.toString(this.initialMultiplier));
      this.addRenderableWidget(this.multiplier);
      this.addRenderableWidget(new NeonButton(x, y + 28, 76, 20, Component.translatable("gui.typemoonworld.overlay.time_alter.mode.accel.short"), b -> this.setMode(0), GuiUtils.ARCANE_CYAN).setArcaneStyle(true));
      this.addRenderableWidget(new NeonButton(x + 84, y + 28, 76, 20, Component.translatable("gui.typemoonworld.overlay.time_alter.mode.stagnate.short"), b -> this.setMode(1), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
      this.addRenderableWidget(new NeonButton(x, y + 56, 160, 20, Component.translatable("gui.done"), b -> this.save(), GuiUtils.ARCANE_VALID).setArcaneStyle(true));
   }

   private void setMode(int mode) {
      PacketDistributor.sendToServer(new MagicModeSwitchMessage(10, mode), new CustomPacketPayload[0]);
   }

   private void save() {
      try {
         int value = Integer.parseInt(this.multiplier.getValue());
         if (value >= 1) {
            PacketDistributor.sendToServer(new SetTimeAlterMultiplierMessage(value), new CustomPacketPayload[0]);
            this.onClose();
         }
      } catch (NumberFormatException ignored) {
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui, mouseX, mouseY, partialTick);
      GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
      int panelX = this.width / 2 - 100;
      int panelY = this.height / 2 - 70;
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, 200, 124, GuiUtils.ARCANE_CYAN);
      gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
      gui.drawString(this.font, Component.translatable("gui.typemoonworld.time_alter.multiplier"), this.width / 2 - 80, this.height / 2 - 54, GuiUtils.ARCANE_TEXT_MUTED);
      super.render(gui, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
