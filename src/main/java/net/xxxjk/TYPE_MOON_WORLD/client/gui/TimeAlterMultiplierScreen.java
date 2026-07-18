package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
      this.addRenderableWidget(Button.builder(Component.translatable("gui.typemoonworld.overlay.time_alter.mode.accel.short"), b -> this.setMode(0))
         .bounds(x, y + 28, 76, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.typemoonworld.overlay.time_alter.mode.stagnate.short"), b -> this.setMode(1))
         .bounds(x + 84, y + 28, 76, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.save())
         .bounds(x, y + 56, 160, 20).build());
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
      gui.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 66, 0xFFFFFFFF);
      gui.drawString(this.font, Component.translatable("gui.typemoonworld.time_alter.multiplier"), this.width / 2 - 80, this.height / 2 - 54, 0xFFBFE8FF);
      super.render(gui, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
