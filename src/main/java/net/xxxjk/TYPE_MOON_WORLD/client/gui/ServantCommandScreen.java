package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.CommandSpellVisualClient;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCommandSpellMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCommandSpellPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public class ServantCommandScreen extends Screen {
   private final int entityId;
   private int panelX;
   private int panelY;

   public ServantCommandScreen(int entityId) {
      super(Component.translatable("gui.typemoonworld.servant_command.title"));
      this.entityId = entityId;
   }

   @Override
   protected void init() {
      setPoseActive(true);
      int panelWidth = 444;
      int panelHeight = 268;
      panelX = (width - panelWidth) / 2;
      panelY = (height - panelHeight) / 2;
      int columnWidth = 202;
      int gap = 18;
      int leftX = panelX + 12;
      int rightX = leftX + columnWidth + gap;
      int buttonY = panelY + 48;

      addRenderableWidget(new NeonButton(leftX, buttonY, columnWidth, 20,
         Component.translatable("gui.typemoonworld.master_command_spell.restore_mana"), button -> cast(0), GuiUtils.ARCANE_CREST).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(leftX, buttonY + 25, columnWidth, 20,
         Component.translatable("gui.typemoonworld.master_command_spell.teleport"), button -> cast(1), GuiUtils.ARCANE_CREST).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(leftX, buttonY + 50, columnWidth, 20,
         Component.translatable("gui.typemoonworld.master_command_spell.suicide"), button -> cast(2), GuiUtils.ARCANE_DANGER).setArcaneStyle(true));

      addRenderableWidget(new NeonButton(rightX, buttonY, columnWidth, 20,
         Component.translatable("gui.typemoonworld.servant_command.follow"), button -> command(0), GuiUtils.ARCANE_CYAN).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(rightX, buttonY + 25, columnWidth, 20,
         Component.translatable("gui.typemoonworld.servant_command.guard"), button -> command(1), GuiUtils.ARCANE_CYAN).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(rightX, buttonY + 50, columnWidth, 20,
         Component.translatable("gui.typemoonworld.servant_command.stay"), button -> command(2), GuiUtils.ARCANE_CYAN).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(rightX, buttonY + 75, columnWidth, 20,
         Component.translatable("gui.typemoonworld.servant_command.np_permission"), button -> command(3), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(rightX, buttonY + 100, columnWidth, 20,
         Component.translatable("gui.typemoonworld.servant_command.combat_cycle"), button -> command(4), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
      addRenderableWidget(new NeonButton(rightX, buttonY + 125, columnWidth, 20,
         Component.translatable("gui.typemoonworld.servant_command.social_cycle"), button -> command(5), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
   }

   @Override
   public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      renderBackground(gui, mouseX, mouseY, partialTick);
      GuiUtils.renderScreenBackdrop(gui, width, height);
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, 444, 268, GuiUtils.ARCANE_CYAN);
      ServantEntity servant = getServant();
      Component name = servant == null
         ? Component.translatable("gui.typemoonworld.servant_command.invalid")
         : servant.getDisplayName();
      gui.drawCenteredString(font, name, width / 2, panelY + 8, GuiUtils.ARCANE_TEXT);
      gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command.spells"), panelX + 14, panelY + 34, GuiUtils.ARCANE_CREST);
      gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command.controls"), panelX + 234, panelY + 34, GuiUtils.ARCANE_CYAN);

      int infoY = panelY + 185;
      gui.fill(panelX + 10, infoY - 5, panelX + 434, infoY - 4, 0x5534404C);
      gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command.behavior"), panelX + 14, infoY, GuiUtils.ARCANE_GOLD);
      if (servant != null) {
         drawInfo(gui, "obedience", servant.getObedienceAxis().key(), panelX + 14, infoY + 15);
         drawInfo(gui, "principle", servant.getPrincipleAxis().key(), panelX + 14, infoY + 29);
         drawInfo(gui, "social", servant.getSocialDisposition().key(), panelX + 225, infoY + 15);
         drawInfo(gui, "combat", servant.getCombatDisposition().key(), panelX + 225, infoY + 29);
         gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command.favor", Math.round((float) servant.getFavor())),
            panelX + 14, infoY + 43, GuiUtils.ARCANE_TEXT_MUTED);
         gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command.mode",
               Component.translatable("gui.typemoonworld.servant_command.mode." + servant.getCommandMode().name().toLowerCase())),
            panelX + 225, infoY + 43, GuiUtils.ARCANE_TEXT_MUTED);
         gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command.np_state",
               Component.translatable(servant.hasMasterNoblePhantasmPermission()
                  ? "gui.typemoonworld.servant_command.allowed" : "gui.typemoonworld.servant_command.forbidden")),
            panelX + 14, infoY + 57, GuiUtils.ARCANE_TEXT_MUTED);
      }
      super.render(gui, mouseX, mouseY, partialTick);
   }

   private void drawInfo(GuiGraphics gui, String key, String value, int x, int y) {
      gui.drawString(font, Component.translatable("gui.typemoonworld.servant_command." + key,
         Component.translatable("gui.typemoonworld.servant_command.value." + value)), x, y, GuiUtils.ARCANE_TEXT_MUTED);
   }

   private ServantEntity getServant() {
      Minecraft mc = Minecraft.getInstance();
      Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
      return entity instanceof ServantEntity servant ? servant : null;
   }

   private void cast(int action) {
      PacketDistributor.sendToServer(new MasterCommandSpellMessage(action), new CustomPacketPayload[0]);
      onClose();
   }

   private void command(int action) {
      PacketDistributor.sendToServer(new ServantCommandMessage(entityId, action), new CustomPacketPayload[0]);
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

   private static void setPoseActive(boolean active) {
      CommandSpellVisualClient.setLocalCommandSpellPoseActive(active);
      PacketDistributor.sendToServer(new MasterCommandSpellPoseMessage(active), new CustomPacketPayload[0]);
   }
}
