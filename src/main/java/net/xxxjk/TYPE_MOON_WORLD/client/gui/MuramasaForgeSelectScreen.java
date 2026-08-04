package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.MuramasaForgeSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MuramasaCombatHelper;
import org.jetbrains.annotations.NotNull;

public final class MuramasaForgeSelectScreen extends Screen {
   private int selected;

   public MuramasaForgeSelectScreen() {
      super(Component.translatable("gui.typemoonworld.muramasa_forge.title"));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int index = indexAt(mouseX, mouseY);
         if (index >= 0) {
            PacketDistributor.sendToServer(new MuramasaForgeSelectionMessage(index), new CustomPacketPayload[0]);
            onClose();
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         onClose();
         return true;
      }
      if (keyCode == 263 || keyCode == 264) {
         selected = (selected + 3) % 4;
      }
      if (keyCode == 262 || keyCode == 265) {
         selected = (selected + 1) % 4;
      }
      if (keyCode == 257 || keyCode == 335) {
         PacketDistributor.sendToServer(new MuramasaForgeSelectionMessage(selected), new CustomPacketPayload[0]);
         onClose();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, width, height);
      int tileWidth = 92;
      int tileHeight = 82;
      int gap = 8;
      int startX = (width - tileWidth * 4 - gap * 3) / 2;
      int y = height / 2 - tileHeight / 2;
      graphics.drawCenteredString(font, title, width / 2, y - 28, 0xFFFFFFFF);
      for (int i = 0; i < 4; i++) {
         int x = startX + i * (tileWidth + gap);
         boolean active = i == selected || indexAt(mouseX, mouseY) == i;
         GuiUtils.renderChoiceTile(graphics, x, y, tileWidth, tileHeight, 0xFFC58BE2, active, true);

         ItemStack stack = stackFor(i);
         graphics.pose().pushPose();
         graphics.pose().translate(x + tileWidth / 2.0F - 16.0F, y + 3.0F, 0.0F);
         graphics.pose().scale(2.0F, 2.0F, 2.0F);
         graphics.renderItem(stack, 0, 0);
         graphics.pose().popPose();

         graphics.drawCenteredString(font, stack.getHoverName(), x + tileWidth / 2, y + 42, 0xFFFFFFFF);
         graphics.drawCenteredString(
            font,
            Component.translatable("gui.typemoonworld.muramasa_forge.select"),
            x + tileWidth / 2,
            y + 61,
            0xFFC7F5FF
         );
      }
   }

   private static ItemStack stackFor(int index) {
      Item item = switch (index) {
         case 0 -> ModItems.MURAMASA.get();
         case 1 -> ModItems.WAKIZASHI.get();
         case 2 -> ModItems.KATANA.get();
         default -> ModItems.NODACHI.get();
      };
      return MuramasaCombatHelper.projectedStack(item);
   }

   private int indexAt(double mouseX, double mouseY) {
      int tileWidth = 92;
      int tileHeight = 82;
      int gap = 8;
      int startX = (width - tileWidth * 4 - gap * 3) / 2;
      int y = height / 2 - tileHeight / 2;
      for (int i = 0; i < 4; i++) {
         int x = startX + i * (tileWidth + gap);
         if (mouseX >= x && mouseX < x + tileWidth && mouseY >= y && mouseY < y + tileHeight) {
            return i;
         }
      }
      return -1;
   }
}
