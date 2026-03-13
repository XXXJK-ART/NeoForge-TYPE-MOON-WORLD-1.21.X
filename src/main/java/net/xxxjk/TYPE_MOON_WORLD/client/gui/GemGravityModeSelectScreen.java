package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.GemGravitySelfCastMessage;
import org.jetbrains.annotations.NotNull;

public class GemGravityModeSelectScreen extends Screen {
   private final int handValue;
   private final List<Component> modes = new ArrayList<>();
   private final List<Integer> modeIds = new ArrayList<>();
   private int selectedIndex = 0;
   private static final ResourceLocation ICON_ULTRA_LIGHT = ResourceLocation.withDefaultNamespace("textures/mob_effect/jump_boost.png");
   private static final ResourceLocation ICON_LIGHT = ResourceLocation.withDefaultNamespace("textures/mob_effect/slow_falling.png");
   private static final ResourceLocation ICON_NORMAL = ResourceLocation.withDefaultNamespace("textures/mob_effect/glowing.png");
   private static final ResourceLocation ICON_HEAVY = ResourceLocation.withDefaultNamespace("textures/mob_effect/slowness.png");
   private static final ResourceLocation ICON_ULTRA_HEAVY = ResourceLocation.withDefaultNamespace("textures/mob_effect/mining_fatigue.png");

   public GemGravityModeSelectScreen(InteractionHand hand) {
      super(Component.translatable("gui.typemoonworld.gem.gravity_select.title"));
      this.handValue = hand == InteractionHand.OFF_HAND ? 1 : 0;
      this.addMode(-2, Component.translatable("gui.typemoonworld.mode.gravity.ultra_light"));
      this.addMode(-1, Component.translatable("gui.typemoonworld.mode.gravity.light"));
      this.addMode(0, Component.translatable("gui.typemoonworld.mode.gravity.normal"));
      this.addMode(1, Component.translatable("gui.typemoonworld.mode.gravity.heavy"));
      this.addMode(2, Component.translatable("gui.typemoonworld.mode.gravity.ultra_heavy"));
   }

   private void addMode(int id, Component component) {
      this.modeIds.add(id);
      this.modes.add(component);
   }

   protected void init() {
      super.init();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY > 0.0) {
         this.selectedIndex = (this.selectedIndex - 1 + this.modes.size()) % this.modes.size();
      } else if (scrollY < 0.0) {
         this.selectedIndex = (this.selectedIndex + 1) % this.modes.size();
      }

      return true;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.updateSelectionAt(mouseX, mouseY)) {
         int mode = this.modeIds.get(this.selectedIndex);
         PacketDistributor.sendToServer(new GemGravitySelfCastMessage(this.handValue, 0, mode), new CustomPacketPayload[0]);
         this.onClose();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int itemWidth = 50;
      int itemHeight = 50;
      int gap = 8;
      int totalWidth = itemWidth * this.modes.size() + gap * (this.modes.size() - 1);
      int startX = (this.width - totalWidth) / 2;
      int centerY = this.height / 2;
      int startY = centerY - itemHeight / 2;
      int padding = 10;
      int bgX1 = startX - padding;
      int bgY1 = startY - padding - 20;
      int bgX2 = startX + totalWidth + padding;
      int bgY2 = startY + itemHeight + padding;
      guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, -1275068416);
      guiGraphics.renderOutline(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, -7829368);
      this.updateSelectionAt(mouseX, mouseY);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 5, 16777215);

      for (int i = 0; i < this.modes.size(); i++) {
         int x = startX + i * (itemWidth + gap);
         boolean selected = i == this.selectedIndex;
         int fillColor = selected ? 1627389951 : 1073741824;
         int textColor = selected ? -171 : -5592406;
         int borderColor = selected ? -1 : -11184811;
         guiGraphics.fill(x, startY, x + itemWidth, startY + itemHeight, fillColor);
         guiGraphics.renderOutline(x, startY, itemWidth, itemHeight, borderColor);
         ResourceLocation icon = this.getIconByMode(this.modeIds.get(i));
         if (icon != null) {
            int iconSize = 24;
            int iconX = x + (itemWidth - iconSize) / 2;
            int iconY = startY + 4;
            RenderSystem.enableBlend();
            guiGraphics.setColor(0.54F, 0.49F, 1.0F, 0.75F);
            guiGraphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
         }

         guiGraphics.drawCenteredString(this.font, this.modes.get(i), x + itemWidth / 2, startY + itemHeight - 10, textColor);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private ResourceLocation getIconByMode(int mode) {
      return switch (mode) {
         case -2 -> ICON_ULTRA_LIGHT;
         case -1 -> ICON_LIGHT;
         case 0 -> ICON_NORMAL;
         case 1 -> ICON_HEAVY;
         case 2 -> ICON_ULTRA_HEAVY;
         default -> null;
      };
   }

   private boolean updateSelectionAt(double mouseX, double mouseY) {
      int itemWidth = 50;
      int itemHeight = 50;
      int gap = 8;
      int totalWidth = itemWidth * this.modes.size() + gap * (this.modes.size() - 1);
      int startX = (this.width - totalWidth) / 2;
      int centerY = this.height / 2;
      int startY = centerY - itemHeight / 2;

      for (int i = 0; i < this.modes.size(); i++) {
         int x = startX + i * (itemWidth + gap);
         if (mouseX >= x && mouseX < x + itemWidth && mouseY >= startY && mouseY < startY + itemHeight) {
            this.selectedIndex = i;
            return true;
         }
      }

      return false;
   }
}
