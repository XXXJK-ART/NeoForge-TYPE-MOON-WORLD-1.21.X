package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.CommandSpellVisualClient;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCommandSpellMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCommandSpellPoseMessage;

public class MasterCommandSpellScreen extends Screen {
   public MasterCommandSpellScreen() {
      super(Component.translatable("gui.typemoonworld.master_command_spell.title"));
   }

   @Override
   protected void init() {
      setPoseActive(true);
      int width = 180;
      int height = 20;
      int x = (this.width - width) / 2;
      int y = this.height / 2 - 36;
      this.addRenderableWidget(new NeonButton(x, y, width, height, Component.translatable("gui.typemoonworld.master_command_spell.restore_mana"), button -> this.cast(0), GuiUtils.ARCANE_CREST).setArcaneStyle(true));
      this.addRenderableWidget(new NeonButton(x, y + 26, width, height, Component.translatable("gui.typemoonworld.master_command_spell.teleport"), button -> this.cast(1), GuiUtils.ARCANE_CREST).setArcaneStyle(true));
      this.addRenderableWidget(new NeonButton(x, y + 52, width, height, Component.translatable("gui.typemoonworld.master_command_spell.suicide"), button -> this.cast(2), GuiUtils.ARCANE_DANGER).setArcaneStyle(true));
      if (this.minecraft != null && this.minecraft.player != null
         && "supervisor".equals(CommandSpellVisualClient.getCommandSpellStyle(this.minecraft.player))) {
         this.addRenderableWidget(new NeonButton(x, y + 78, width, height, Component.translatable("gui.typemoonworld.master_command_spell.extract"), button -> this.cast(3), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui, mouseX, mouseY, partialTick);
      GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
      int panelY = this.height / 2 - 76;
      GuiUtils.renderArcaneWindow(gui, this.width / 2 - 102, panelY, 204, 142, GuiUtils.ARCANE_CREST);
      gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
      super.render(gui, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void onClose() {
      setPoseActive(false);
      super.onClose();
   }

   private void cast(int action) {
      PacketDistributor.sendToServer(new MasterCommandSpellMessage(action), new CustomPacketPayload[0]);
      this.onClose();
   }

   private static void setPoseActive(boolean active) {
      CommandSpellVisualClient.setLocalCommandSpellPoseActive(active);
      PacketDistributor.sendToServer(new MasterCommandSpellPoseMessage(active), new CustomPacketPayload[0]);
   }
}
