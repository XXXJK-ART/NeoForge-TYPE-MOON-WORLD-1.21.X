package net.xxxjk.TYPE_MOON_WORLD.effect;

import java.util.function.Consumer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

public class ReinforcementEffect extends UncurableEffect {
   private final ResourceLocation iconTexture;
   private final float red;
   private final float green;
   private final float blue;

   public ReinforcementEffect(MobEffectCategory category, int color, ResourceLocation iconTexture) {
      super(category, color);
      this.iconTexture = iconTexture;
      this.red = (color >> 16 & 0xFF) / 255.0F;
      this.green = (color >> 8 & 0xFF) / 255.0F;
      this.blue = (color & 0xFF) / 255.0F;
   }

   @SuppressWarnings("removal")
   public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
      consumer.accept(
         new IClientMobEffectExtensions() {
            public boolean renderInventoryIcon(
               MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset
            ) {
               guiGraphics.setColor(0.0F, 1.0F, 1.0F, 0.5F);
               guiGraphics.blit(ReinforcementEffect.this.iconTexture, x, y + 7, 0.0F, 0.0F, 18, 18, 18, 18);
               guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
               return true;
            }

            public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
               guiGraphics.setColor(0.0F, 1.0F, 1.0F, alpha * 0.5F);
               guiGraphics.blit(ReinforcementEffect.this.iconTexture, x + 3, y + 3, 0.0F, 0.0F, 18, 18, 18, 18);
               guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
               return true;
            }
         }
      );
   }
}
