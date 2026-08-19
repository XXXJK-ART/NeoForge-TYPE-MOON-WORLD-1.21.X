package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.MedeaCraftSelectionMessage;
import org.jetbrains.annotations.NotNull;

public class MedeaCraftSelectScreen extends Screen {
   private static final int MAX_DRAGONFANG = 50;
   private static final int MAX_MANA_CHARM = 10;
   private static final int MAX_HEAL_CHARM = 10;
   private static final int CHOICE_COUNT = 6;
   private static final ResourceLocation DRAGONFANG_ICON = ResourceLocation.withDefaultNamespace("textures/item/bone.png");
   private static final ResourceLocation MANA_CHARM_ICON = ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png");
   private static final ResourceLocation HEAL_CHARM_ICON = ResourceLocation.withDefaultNamespace("textures/item/golden_apple.png");
   private static final ResourceLocation LEYLINE_MAP_ICON = ResourceLocation.withDefaultNamespace("textures/item/map.png");
   private static final ResourceLocation REINFORCEMENT_CHARM_ICON = ResourceLocation.withDefaultNamespace("textures/item/iron_nugget.png");
   private static final ResourceLocation SERVANT_CONTRACT_ICON = ResourceLocation.withDefaultNamespace("textures/item/paper.png");
   private final int dragonfangStock;
   private final int manaCharmStock;
   private final int healCharmStock;
   private final int leylineMapStock;
   private final int reinforcementCharmStock;
   private final int servantContractStock;
   private int selectedIndex = 0;

   public MedeaCraftSelectScreen(int dragonfangStock, int manaCharmStock, int healCharmStock, int leylineMapStock, int reinforcementCharmStock, int servantContractStock) {
      super(Component.translatable("gui.typemoonworld.medea_craft.title"));
      this.dragonfangStock = Math.max(0, dragonfangStock);
      this.manaCharmStock = Math.max(0, manaCharmStock);
      this.healCharmStock = Math.max(0, healCharmStock);
      this.leylineMapStock = Math.max(0, leylineMapStock);
      this.reinforcementCharmStock = Math.max(0, reinforcementCharmStock);
      this.servantContractStock = Math.max(0, servantContractStock);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.updateSelectionAt(mouseX, mouseY)) {
         if (!this.isSelectedFull()) {
            PacketDistributor.sendToServer(new MedeaCraftSelectionMessage(this.selectedIndex), new CustomPacketPayload[0]);
            this.onClose();
         }
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY > 0.0) {
         this.selectedIndex = (this.selectedIndex + CHOICE_COUNT - 1) % CHOICE_COUNT;
      } else if (scrollY < 0.0) {
         this.selectedIndex = (this.selectedIndex + 1) % CHOICE_COUNT;
      }
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.onClose();
         return true;
      }
      if (keyCode == 257 || keyCode == 335) {
         if (!this.isSelectedFull()) {
            PacketDistributor.sendToServer(new MedeaCraftSelectionMessage(this.selectedIndex), new CustomPacketPayload[0]);
            this.onClose();
         }
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int itemWidth = 82;
      int itemHeight = 56;
      int gap = 8;
      int columns = 3;
      int rows = 2;
      int totalWidth = itemWidth * columns + gap * (columns - 1);
      int startX = (this.width - totalWidth) / 2;
      int startY = this.height / 2 - (itemHeight * rows + gap) / 2;
      int bgX1 = startX - 12;
      int bgY1 = startY - 28;
      int bgX2 = startX + totalWidth + 12;
      int bgY2 = startY + itemHeight * rows + gap + 12;

      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      GuiUtils.renderScreenBackdrop(guiGraphics, this.width, this.height);
      GuiUtils.renderArcaneWindow(guiGraphics, bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 0xFFC58BE2);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 8, 0xFFEDE6FF);
      this.updateSelectionAt(mouseX, mouseY);

      for (int i = 0; i < CHOICE_COUNT; i++) {
         int x = startX + (i % columns) * (itemWidth + gap);
         int y = startY + (i / columns) * (itemHeight + gap);
         this.renderChoice(guiGraphics, i, x, y, itemWidth, itemHeight);
      }
   }

   private void renderChoice(GuiGraphics guiGraphics, int index, int x, int y, int width, int height) {
      boolean selected = index == this.selectedIndex;
      boolean full = this.isFull(index);
      int text = full ? 0xFFB0A0A0 : selected ? 0xFFFFFFFF : 0xFFD7C9EE;
      GuiUtils.renderChoiceTile(guiGraphics, x, y, width, height, 0xFFC58BE2, selected, !full);

      ResourceLocation icon = this.icon(index);
      RenderSystem.enableBlend();
      guiGraphics.setColor(1.0F, 1.0F, 1.0F, full ? 0.45F : 0.86F);
      guiGraphics.blit(icon, x + 7, y + 10, 0.0F, 0.0F, 16, 16, 16, 16);
      guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();

      guiGraphics.drawString(this.font, this.label(index), x + 29, y + 7, text, true);
      guiGraphics.drawString(this.font, this.count(index), x + 29, y + 22, full ? 0xFFFFB0A0 : 0xFFC7F5FF, true);
      guiGraphics.drawCenteredString(
         this.font,
         full ? Component.translatable("gui.typemoonworld.medea_craft.full") : Component.translatable("gui.typemoonworld.medea_craft.select"),
         x + width / 2,
         y + 43,
         full ? 0xFFFFA0A0 : 0xFFBFEFBF
      );
   }

   private boolean updateSelectionAt(double mouseX, double mouseY) {
      int itemWidth = 82;
      int itemHeight = 56;
      int gap = 8;
      int columns = 3;
      int totalWidth = itemWidth * columns + gap * (columns - 1);
      int startX = (this.width - totalWidth) / 2;
      int startY = this.height / 2 - (itemHeight * 2 + gap) / 2;
      for (int i = 0; i < CHOICE_COUNT; i++) {
         int x = startX + (i % columns) * (itemWidth + gap);
         int y = startY + (i / columns) * (itemHeight + gap);
         if (mouseX >= x && mouseX < x + itemWidth && mouseY >= y && mouseY < y + itemHeight) {
            this.selectedIndex = i;
            return true;
         }
      }
      return false;
   }

   private boolean isSelectedFull() {
      return this.isFull(this.selectedIndex);
   }

   private boolean isFull(int index) {
      return switch (index) {
         case 0 -> this.dragonfangStock >= MAX_DRAGONFANG;
         case 1 -> this.manaCharmStock >= MAX_MANA_CHARM;
         case 2 -> this.healCharmStock >= MAX_HEAL_CHARM;
         case 3 -> false;
         case 4, 5 -> false;
         default -> true;
      };
   }

   private Component label(int index) {
      return switch (index) {
         case 0 -> Component.translatable("hud.typemoonworld.servant_card.medea_dragonfang");
         case 1 -> Component.translatable("hud.typemoonworld.servant_card.medea_mana_charm");
         case 2 -> Component.translatable("hud.typemoonworld.servant_card.medea_heal_charm");
         case 3 -> Component.translatable("item.typemoonworld.leyline_survey_map");
         case 4 -> Component.translatable("item.typemoonworld.medea_reinforcement_charm");
         case 5 -> Component.translatable("item.typemoonworld.medea_servant_contract");
         default -> Component.empty();
      };
   }

   private Component count(int index) {
      return switch (index) {
         case 0 -> Component.literal(this.dragonfangStock + "/" + MAX_DRAGONFANG);
         case 1 -> Component.literal(this.manaCharmStock + "/" + MAX_MANA_CHARM);
         case 2 -> Component.literal(this.healCharmStock + "/" + MAX_HEAL_CHARM);
         case 3 -> Component.literal(String.valueOf(this.leylineMapStock));
         case 4 -> Component.literal(String.valueOf(this.reinforcementCharmStock));
         case 5 -> Component.literal(String.valueOf(this.servantContractStock));
         default -> Component.empty();
      };
   }

   private ResourceLocation icon(int index) {
      return switch (index) {
         case 1 -> MANA_CHARM_ICON;
         case 2 -> HEAL_CHARM_ICON;
         case 3 -> LEYLINE_MAP_ICON;
         case 4 -> REINFORCEMENT_CHARM_ICON;
         case 5 -> SERVANT_CONTRACT_ICON;
         default -> DRAGONFANG_ICON;
      };
   }
}
