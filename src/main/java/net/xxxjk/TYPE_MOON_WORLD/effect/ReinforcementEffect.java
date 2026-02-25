package net.xxxjk.TYPE_MOON_WORLD.effect;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

public class ReinforcementEffect extends UncurableEffect {
    private final ResourceLocation iconTexture;
    private final float red;
    private final float green;
    private final float blue;

    public ReinforcementEffect(MobEffectCategory category, int color, ResourceLocation iconTexture) {
        super(category, color);
        this.iconTexture = iconTexture;
        // Extract RGB from color int
        this.red = ((color >> 16) & 0xFF) / 255.0f;
        this.green = ((color >> 8) & 0xFF) / 255.0f;
        this.blue = (color & 0xFF) / 255.0f;
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
                guiGraphics.setColor(0.0f, 1.0f, 1.0f, 0.5f);
                // Shifted right to fix offset (Standard x+6 + 3/4 of 24 = x+24)
                guiGraphics.blit(iconTexture, x, y + 7, 0, 0, 18, 18, 18, 18); 
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
                guiGraphics.setColor(0.0f, 1.0f, 1.0f, alpha * 0.5f);
                // Shifted right and down to fix offset (Standard x+3 + half of 12 = x+9)
                guiGraphics.blit(iconTexture, x + 3, y + 3, 0, 0, 18, 18, 18, 18);
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                return true;
            }
        });
    }
}
