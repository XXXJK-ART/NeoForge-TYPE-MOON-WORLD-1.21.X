package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.ParacelsusCraftSelectionMessage;
import org.jetbrains.annotations.NotNull;

public class ParacelsusCraftSelectScreen extends Screen {
   private static final int MAX_STONE = 5;
   private static final int MAX_DIAMOND_SHIELD = 3;
   private static final int CHOICE_COUNT = 3;
   private static final ResourceLocation STONE_ICON = ResourceLocation.withDefaultNamespace("textures/item/nether_star.png");
   private static final ResourceLocation SHIELD_ICON = ResourceLocation.withDefaultNamespace("textures/item/diamond.png");
   private static final ResourceLocation LEYLINE_MAP_ICON = ResourceLocation.withDefaultNamespace("textures/item/map.png");
   private final int stoneStock;
   private final int diamondShieldStock;
   private final int leylineMapStock;
   private int selectedIndex = 0;

   public ParacelsusCraftSelectScreen(int stoneStock, int diamondShieldStock, int leylineMapStock) {
      super(Component.translatable("gui.typemoonworld.paracelsus_craft.title"));
      this.stoneStock = Math.max(0, stoneStock);
      this.diamondShieldStock = Math.max(0, diamondShieldStock);
      this.leylineMapStock = Math.max(0, leylineMapStock);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.updateSelectionAt(mouseX, mouseY)) {
         if (!this.isSelectedFull()) {
            PacketDistributor.sendToServer(new ParacelsusCraftSelectionMessage(this.selectedIndex), new CustomPacketPayload[0]);
            this.onClose();
         }
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0.0) {
         this.selectedIndex = scrollY > 0.0 ? (this.selectedIndex + CHOICE_COUNT - 1) % CHOICE_COUNT : (this.selectedIndex + 1) % CHOICE_COUNT;
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
            PacketDistributor.sendToServer(new ParacelsusCraftSelectionMessage(this.selectedIndex), new CustomPacketPayload[0]);
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
      int itemWidth = 104;
      int itemHeight = 58;
      int gap = 12;
      int totalWidth = itemWidth * CHOICE_COUNT + gap * (CHOICE_COUNT - 1);
      int startX = (this.width - totalWidth) / 2;
      int startY = this.height / 2 - itemHeight / 2;
      int bgX1 = startX - 12;
      int bgY1 = startY - 28;
      int bgX2 = startX + totalWidth + 12;
      int bgY2 = startY + itemHeight + 12;

      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      GuiUtils.renderScreenBackdrop(guiGraphics, this.width, this.height);
      GuiUtils.renderArcaneWindow(guiGraphics, bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 0xFF9ADBE8);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 8, 0xFFEDE6FF);
      this.updateSelectionAt(mouseX, mouseY);

      for (int i = 0; i < CHOICE_COUNT; i++) {
         int x = startX + i * (itemWidth + gap);
         this.renderChoice(guiGraphics, i, x, startY, itemWidth, itemHeight);
      }
   }

   private void renderChoice(GuiGraphics guiGraphics, int index, int x, int y, int width, int height) {
      boolean selected = index == this.selectedIndex;
      boolean full = this.isFull(index);
      int text = full ? 0xFFB0A0A0 : selected ? 0xFFFFFFFF : 0xFFD7EFFF;
      GuiUtils.renderChoiceTile(guiGraphics, x, y, width, height, 0xFF9ADBE8, selected, !full);

      RenderSystem.enableBlend();
      guiGraphics.setColor(1.0F, 1.0F, 1.0F, full ? 0.45F : 0.86F);
      guiGraphics.blit(this.icon(index), x + 7, y + 10, 0.0F, 0.0F, 16, 16, 16, 16);
      guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();

      guiGraphics.drawString(this.font, this.label(index), x + 29, y + 7, text, true);
      guiGraphics.drawString(this.font, this.count(index), x + 29, y + 22, full ? 0xFFFFB0A0 : 0xFFC7F5FF, true);
      guiGraphics.drawCenteredString(
         this.font,
         full ? Component.translatable("gui.typemoonworld.paracelsus_craft.full") : Component.translatable("gui.typemoonworld.paracelsus_craft.select"),
         x + width / 2,
         y + 43,
         full ? 0xFFFFA0A0 : 0xFFBFEFBF
      );
   }

   private boolean updateSelectionAt(double mouseX, double mouseY) {
      int itemWidth = 104;
      int itemHeight = 58;
      int gap = 12;
      int totalWidth = itemWidth * CHOICE_COUNT + gap * (CHOICE_COUNT - 1);
      int startX = (this.width - totalWidth) / 2;
      int startY = this.height / 2 - itemHeight / 2;
      for (int i = 0; i < CHOICE_COUNT; i++) {
         int x = startX + i * (itemWidth + gap);
         if (mouseX >= x && mouseX < x + itemWidth && mouseY >= startY && mouseY < startY + itemHeight) {
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
         case 0 -> this.stoneStock >= MAX_STONE;
         case 1 -> this.diamondShieldStock >= MAX_DIAMOND_SHIELD;
         case 2 -> false;
         default -> true;
      };
   }

   private Component label(int index) {
      return switch (index) {
         case 0 -> Component.translatable("hud.typemoonworld.servant_card.paracelsus_stone");
         case 1 -> Component.translatable("hud.typemoonworld.servant_card.paracelsus_diamond_shield");
         case 2 -> Component.translatable("item.typemoonworld.leyline_survey_map");
         default -> Component.empty();
      };
   }

   private Component count(int index) {
      return switch (index) {
         case 0 -> Component.literal(this.stoneStock + "/" + MAX_STONE);
         case 1 -> Component.literal(this.diamondShieldStock + "/" + MAX_DIAMOND_SHIELD);
         case 2 -> Component.literal(String.valueOf(this.leylineMapStock));
         default -> Component.empty();
      };
   }

   private ResourceLocation icon(int index) {
      return switch (index) {
         case 0 -> STONE_ICON;
         case 1 -> SHIELD_ICON;
         case 2 -> LEYLINE_MAP_ICON;
         default -> STONE_ICON;
      };
   }
}
