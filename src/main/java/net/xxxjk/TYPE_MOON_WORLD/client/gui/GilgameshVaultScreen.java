package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.GilgameshVaultSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardGilgameshSkills;

public final class GilgameshVaultScreen extends Screen {
   private static final int SLOT = 28;
   private final int usedMask;

   public GilgameshVaultScreen(int usedMask) {
      super(Component.translatable("gui.typemoonworld.gilgamesh_vault.title"));
      this.usedMask = usedMask;
   }

   @Override
   public boolean isPauseScreen() { return false; }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      super.render(gui, mouseX, mouseY, partialTick);
      int cx = this.width / 2;
      int cy = this.height / 2;
      gui.fill(cx - 92, cy - 92, cx + 92, cy + 92, 0xC0101018);
      gui.renderOutline(cx - 92, cy - 92, 184, 184, 0xFFD4AF37);
      gui.drawCenteredString(this.font, this.title, cx, cy - 82, 0xFFFFD54F);
      for (int i = 0; i < 7; i++) {
         int x = slotX(i, cx); int y = slotY(i, cy);
         boolean used = (this.usedMask & (1 << i)) != 0;
         boolean hovered = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
         gui.fill(x, y, x + SLOT, y + SLOT, used ? 0xC0444444 : hovered ? 0xC0B8860B : 0xC0303038);
         gui.renderOutline(x, y, SLOT, SLOT, used ? 0xFF777777 : 0xFFFFD54F);
         ItemStack stack = ServantCardGilgameshSkills.treasureFor(i);
         gui.renderItem(stack, x + 6, y + 6);
         if (used) gui.fill(x + 3, y + 3, x + SLOT - 3, y + SLOT - 3, 0x88777777);
         if (hovered) gui.renderTooltip(this.font, stack, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 1) { this.onClose(); return true; }
      if (button == 0) {
         int cx = this.width / 2; int cy = this.height / 2;
         for (int i = 0; i < 7; i++) {
            int x = slotX(i, cx); int y = slotY(i, cy);
            if ((this.usedMask & (1 << i)) == 0 && mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
               PacketDistributor.sendToServer(new GilgameshVaultSelectionMessage(i), new CustomPacketPayload[0]);
               this.onClose(); return true;
            }
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private static int slotX(int index, int cx) { return cx + (int)Math.round(Math.cos(-Math.PI / 2 + index * Math.PI * 2 / 7) * 62) - SLOT / 2; }
   private static int slotY(int index, int cy) { return cy + (int)Math.round(Math.sin(-Math.PI / 2 + index * Math.PI * 2 / 7) * 62) - SLOT / 2; }
}
